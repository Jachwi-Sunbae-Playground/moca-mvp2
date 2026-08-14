# 배포

- 상태: 동작 중
- 현재 배포 환경: `https://api.jachwi-sunbae.kr`
- 문서 성격: 파생
- 대조 대상: `backend/deploy/`, 실제 AWS 파이프라인 구성

전체 구성과 선택 근거는 [배포 아키텍처 설계](../../../docs/operations/deployment-architecture.md)에 있다. 이 문서는 백엔드를 실제로 배포하는 절차와 그 절차가 의존하는 서버 상태를 적는다.

## 배포 경로

`main` 병합이 트리거다. 액세스 키를 만들 수 없으므로 GitHub Actions가 AWS에 직접 배포하지 않고, 제공된 service role로 동작하는 AWS 네이티브 파이프라인을 쓴다.

```text
main 병합
  → CodePipeline(codepipeline-project)  jachwi-sunbae-line
    → Commands 액션 ─ 관리형 환경에서 jar 빌드 ─ BuildArtifact
    → CodeDeploy(codedeploy-project) ─ backend/deploy/appspec.yml
      → EC2 (ec2-project role, CodeDeploy 에이전트)
```

아티팩트 저장소는 `techcourse-project-2026-artifacts`다.

## 저장소에 있는 것

| 파일 | 역할 |
| --- | --- |
| `backend/deploy/appspec.yml` | CodeDeploy 훅 순서를 정의한다 |
| `backend/deploy/jachwi-sunbae.service` | systemd 유닛 |
| `backend/deploy/scripts/` | 배포 훅 스크립트 |

## 테스트를 어디서 실행하는가

**빌드 단계는 테스트를 실행하지 않는다.** `clean bootJar -x test`만 실행한다. 테스트는 GitHub Actions(`.github/workflows/backend-ci.yml`)가 PR과 `main` push에서 실행하므로, 배포되는 커밋은 이미 검증을 통과한 상태다.

같은 테스트를 두 곳에서 돌리지 않는 것이 첫 번째 이유다. 두 번째는 통합 테스트가 Testcontainers로 Docker를 요구해 관리형 빌드 환경에 특권 모드가 필요해지기 때문이다.

## 배포 훅

| 훅 | 하는 일 |
| --- | --- |
| `ApplicationStop` | 서비스 중지를 요청한다. **직전 리비전의 스크립트가 실행되므로 첫 배포에는 실행되지 않는다.** 교체를 이 훅에 의존하지 않는다 |
| `BeforeInstall` | `/opt/jachwi-sunbae`를 비운다. 이 배포가 만들지 않은 파일이 남아 있으면 CodeDeploy가 실패한다 |
| `AfterInstall` | 환경변수 파일과 실행 사용자의 존재를 확인하고, 권한을 맞추고, systemd 유닛을 설치한다 |
| `ApplicationStart` | 서비스를 **재시작**한다. 실제 프로세스 교체를 보장하는 단계다 |
| `ValidateService` | `/actuator/health`가 `UP`이 될 때까지 최대 4분 기다린다. 실패하면 배포를 중단하고 최근 로그를 남긴다 |

## 왜 `start`가 아니라 `restart`인가

`ApplicationStart`는 `systemctl start`가 아니라 `systemctl restart`를 쓴다.

`start`는 서비스가 이미 `active`이면 아무 일도 하지 않는다. `ApplicationStop`이 어떤 이유로든 중지에 실패하면 옛 프로세스가 그대로 남고 새 jar는 실행되지 않는다. 그 상태에서 `ValidateService`가 health를 확인하면 **옛 프로세스가 응답해 배포가 성공으로 기록된다.** 실제로는 아무것도 바뀌지 않았는데 초록불이 뜬다.

실제로 이 일이 있었다. `stop.sh`가 `systemctl list-unit-files` 출력을 grep해 서비스 존재를 검사했는데 그 검사가 어긋나 중지를 건너뛰었고, 이어진 배포가 1초 만에 health를 통과했다.

`ApplicationStop`에 기대는 설계 자체가 옳지 않다. 이 훅은 **직전 리비전의 스크립트**로 실행되므로 첫 배포에서는 아예 실행되지 않고, 직전 리비전의 스크립트가 잘못돼 있으면 동작하지도 않는다. 프로세스 교체는 `restart`가 보장한다.

## 애플리케이션 포트

**운영에서 애플리케이션은 80을 듣는다.** 로컬 기본값 8080과 다르다.

`project-app` 보안 그룹은 `project-lb`에서 오는 80과 443만 허용한다. 8080은 열려 있지 않고, 공용 보안 그룹이라 규칙을 추가하면 다른 팀 인스턴스에도 열린다. 그래서 규칙을 바꾸지 않고 애플리케이션을 80으로 옮겼다.

비루트 계정은 1024 미만 포트에 바인딩할 수 없으므로 systemd 유닛에서 `AmbientCapabilities=CAP_NET_BIND_SERVICE`를 준다. EC2 안에서 iptables로 80을 8080으로 넘기는 방법도 있지만, 그 규칙은 저장소에 남지 않고 재부팅 시 사라져 따로 영속화해야 한다.

포트를 바꾸면 세 곳을 함께 고친다. `application-prod.yml`의 `server.port`, `scripts/validate.sh`의 `HEALTH_URL`, ALB 대상 그룹의 포트다. **대상 그룹은 만든 뒤 포트를 바꿀 수 없다.**

## 서버에 있어야 하는 것

배포는 다음을 전제한다. 없으면 `AfterInstall`에서 멈춘다.

| 대상 | 내용 |
| --- | --- |
| `/etc/jachwi-sunbae/app.env` | 운영 환경변수. `0600`, 소유자 `root:root` |
| 사용자 `jachwi` | 애플리케이션 실행 계정. 로그인 셸이 없다 |
| 디렉터리 `/opt/jachwi-sunbae` | 배포 대상 |
| CodeDeploy 에이전트 | `systemctl status codedeploy-agent`가 `active` |

**환경변수 파일은 배포 산출물에 넣지 않는다.** CodeDeploy가 덮어쓰는 경로 밖에 두어 배포마다 값이 사라지지 않게 한다. systemd가 `EnvironmentFile`로 root 권한에서 읽은 뒤 `jachwi`로 내려가므로 애플리케이션 계정에 읽기 권한을 주지 않는다.

값의 목록과 운영에서 달라지는 부분은 [환경변수](../guides/environment-variables.md)에 있다.

## 빌드를 CodeBuild가 아니라 Commands로 하는 이유

별도 CodeBuild 프로젝트를 두지 않고 CodePipeline의 `Commands` 액션을 쓴다. 같은 계정의 다른 팀이 CodeBuild 경로에서 막혔기 때문이다. `codebuild-project` 서비스 role에 파이프라인 `SourceArtifact`를 읽을 `s3:GetObject` 권한이 없어 빌드 입력을 내려받지 못했고, 팀은 role 정책을 바꿀 수 없다.

`Commands`는 파이프라인 서비스 role을 그대로 쓰므로 이 문제를 겪지 않는다. 대신 빌드 정의가 저장소가 아니라 콘솔에 있다. 파이프라인 설정을 바꾸면 이 문서를 함께 고친다.

빌드 명령은 다음을 한다.

1. Corretto 21을 설치하고 `JAVA_HOME`을 잡는다. **관리형 환경의 기본 Java가 17일 수 있어 버전을 명시한다.**
2. `clean bootJar -x test`로 실행 가능한 jar를 만든다.
3. `-plain.jar`가 아닌 jar를 `app.jar`로 복사한다.
4. `backend/deploy/`의 내용을 작업 디렉터리 루트로 옮긴다. **`appspec.yml`이 아티팩트 최상단에 없으면 CodeDeploy가 배포를 시작하지 못한다.**

출력 아티팩트 `BuildArtifact`에는 `app.jar`, `appspec.yml`, `jachwi-sunbae.service`, `scripts/**/*`가 들어간다. 이 목록을 비워두면 Deploy 단계의 입력이 저장소 원본으로 잡혀 배포가 실패한다.

## 로그 확인

```bash
sudo journalctl -u jachwi-sunbae.service -f
sudo systemctl status jachwi-sunbae.service
```

배포 자체가 실패했다면 EC2의 `/opt/codedeploy-agent/deployment-root/deployment-logs/`를 함께 본다.

배포 결과와 사용자 관찰은 [지정 요구사항 2](../requirements/02-deploy-and-observe.md)에 증거와 함께 기록한다.

## 데이터베이스 변경이 포함된 배포

데이터베이스 변경 절차는 [데이터베이스 마이그레이션 가이드](../guides/database-migrations.md)를 따른다.

1. 대상 환경과 DB를 교차 확인하고 애플리케이션 쓰기를 중단한다.
2. pre-Flyway v1.0 DB라면 스키마·행 수를 기록하고 백업을 별도 DB에 복구해 검증한 뒤 버전 1을 명시적으로 baseline한다.
3. V2~V4를 적용하고 Flyway history, 기존 데이터 보존, backfill과 제약을 검증한다.
4. 애플리케이션을 배포하고 Actuator health와 핵심 smoke test를 확인한 뒤 쓰기를 재개한다.
5. checksum 불일치나 마이그레이션 실패가 있으면 배포와 쓰기 재개를 중단하고 [롤백](rollback.md)의 데이터 유입 시점별 절차를 선택한다.

`baseline-on-migrate`는 배포 환경의 상시 설정으로 두지 않는다. v1.1은 expand/backfill 단계이므로 이전 컬럼과 GOSHIWON 데이터 삭제를 같은 배포에 포함하지 않는다.

운영 RDS는 새로 만든 빈 DB이므로 **첫 배포는 2단계의 baseline 대상이 아니다.** Flyway가 V1부터 최신까지 그대로 적용한다.
