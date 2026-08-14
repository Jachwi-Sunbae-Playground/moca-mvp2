# 배포

- 상태: 구성 중
- 현재 배포 환경: EC2(`project-app` 서브넷) + RDS MySQL. 진입 계층(ALB)과 파이프라인은 구성 중이다
- 문서 성격: 파생
- 대조 대상: `backend/buildspec.yml`, `backend/deploy/`, 실제 AWS 파이프라인 구성

전체 구성과 선택 근거는 [배포 아키텍처 설계](../../../docs/operations/deployment-architecture.md)에 있다. 이 문서는 백엔드를 실제로 배포하는 절차와 그 절차가 의존하는 서버 상태를 적는다.

## 배포 경로

`main` 병합이 트리거다. 액세스 키를 만들 수 없으므로 GitHub Actions가 AWS에 직접 배포하지 않고, 제공된 service role로 동작하는 AWS 네이티브 파이프라인을 쓴다.

```text
main 병합
  → CodePipeline
    → CodeBuild(codebuild-project)  backend/buildspec.yml
    → CodeDeploy(codedeploy-project) backend/deploy/appspec.yml
      → EC2 (ec2-project role, CodeDeploy 에이전트)
```

## 저장소에 있는 것

| 파일 | 역할 |
| --- | --- |
| `backend/buildspec.yml` | `bootJar`로 실행 가능한 jar를 만들고 배포 번들을 조립한다 |
| `backend/deploy/appspec.yml` | CodeDeploy 훅 순서를 정의한다 |
| `backend/deploy/jachwi-sunbae.service` | systemd 유닛 |
| `backend/deploy/scripts/` | 배포 훅 스크립트 |

## 테스트를 어디서 실행하는가

**CodeBuild는 테스트를 실행하지 않는다.** `bootJar`만 실행한다. 테스트는 GitHub Actions(`.github/workflows/backend-ci.yml`)가 PR과 `main` push에서 실행하므로, 배포되는 커밋은 이미 검증을 통과한 상태다.

같은 테스트를 두 곳에서 돌리지 않는 것이 첫 번째 이유다. 두 번째는 통합 테스트가 Testcontainers로 Docker를 요구해 CodeBuild에 특권 모드가 필요해지기 때문이다.

## 배포 훅

| 훅 | 하는 일 |
| --- | --- |
| `ApplicationStop` | 서비스를 중지한다. **직전 리비전의 스크립트가 실행되므로 첫 배포에는 실행되지 않는다** |
| `BeforeInstall` | `/opt/jachwi-sunbae`를 비운다. 이 배포가 만들지 않은 파일이 남아 있으면 CodeDeploy가 실패한다 |
| `AfterInstall` | 환경변수 파일과 실행 사용자의 존재를 확인하고, 권한을 맞추고, systemd 유닛을 설치한다 |
| `ApplicationStart` | 서비스를 시작한다 |
| `ValidateService` | `/actuator/health`가 `UP`이 될 때까지 최대 4분 기다린다. 실패하면 배포를 중단하고 최근 로그를 남긴다 |

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

## 로그 확인

```bash
sudo journalctl -u jachwi-sunbae.service -f
sudo systemctl status jachwi-sunbae.service
```

배포 자체가 실패했다면 EC2의 `/opt/codedeploy-agent/deployment-root/deployment-logs/`를 함께 본다.

## 아직 구성하지 않은 것

- CodeBuild 프로젝트, CodeDeploy 애플리케이션·배포 그룹, CodePipeline은 콘솔에서 만든다.
- ALB와 `api.jachwi-sunbae.kr` DNS는 첫 배포 뒤에 만든다. 앱이 없는 상태로 대상 그룹을 만들면 헬스체크가 실패하는데, 설정 문제인지 앱이 없어서인지 구분할 수 없다.

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
