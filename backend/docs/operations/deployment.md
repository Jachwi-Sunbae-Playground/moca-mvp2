# 배포

- 상태: 동작 중
- 현재 배포 환경: prod `https://api.jachwi-sunbae.kr`, dev `https://dev-api.jachwi-sunbae.kr`
- 문서 성격: 파생
- 대조 대상: `backend/deploy/`, 실제 AWS 파이프라인 구성

전체 구성과 선택 근거는 [배포 아키텍처 설계](../../../docs/operations/deployment-architecture.md)에 있다. 이 문서는 백엔드를 실제로 배포하는 절차와 그 절차가 의존하는 서버 상태를 적는다.

## 배포 경로

브랜치 병합이 트리거다. `main`과 `develop`은 필수 상태 검사를 통과한 PR만 병합할 수 있다. 액세스 키를 만들 수 없으므로 GitHub Actions가 AWS에 직접 배포하지 않고, 제공된 service role로 동작하는 AWS 네이티브 파이프라인을 쓴다.

```text
develop 병합                       main 병합
  → jachwi-sunbae-dev-line           → jachwi-sunbae-line
    → Commands ─ jar 빌드              → Commands ─ jar 빌드
    → CodeDeploy                       → CodeDeploy
        jachwi-sunbae-dev-group            jachwi-sunbae-codeDeploy-group
        DeployTarget=jachwi-sunbae-dev     DeployTarget=jachwi-sunbae-prod
      → dev EC2                          → prod EC2
```

두 파이프라인은 **같은 빌드 명령과 같은 배포 훅**을 쓴다. 다른 것은 소스 브랜치와 배포 그룹뿐이다.

아티팩트 저장소는 두 파이프라인 모두 `techcourse-project-2026-artifacts`다.

### 배포 대상을 가르는 것

CodeDeploy 배포 그룹이 **EC2 태그**로 대상을 고른다.

| 환경 | 태그 | EC2 |
| --- | --- | --- |
| prod | `DeployTarget=jachwi-sunbae-prod` | `i-0f602d10ed2ace6c7` `t4g.small` |
| dev | `DeployTarget=jachwi-sunbae-dev` | `i-068617b197557da19` `t4g.micro` |

**인스턴스를 새로 만들 때 이 태그를 틀리면 배포가 두 환경으로 나간다.** 태그 값이 배포 격리의 유일한 기준이다.

### 요청을 가르는 것

**ALB는 하나다.** 443 리스너의 호스트 기반 규칙이 요청을 나눈다.

| 우선순위 | 조건 | 대상 그룹 |
| --- | --- | --- |
| 1 | `Host = dev-api.jachwi-sunbae.kr` | `jachwi-sunbae-dev-tg` |
| 기본 | 그 외 전부 | `jachwi-sunbe-tg` (prod) |

**기본 작업을 바꾸지 않는다.** 조건에 걸리지 않는 요청은 prod로 간다. 기본 작업을 dev로 바꾸면 prod가 끊긴다.

인증서는 SNI로 두 장을 함께 붙인다.

## 저장소에 있는 것

| 파일 | 역할 |
| --- | --- |
| `backend/deploy/appspec.yml` | CodeDeploy 훅 순서를 정의한다 |
| `backend/deploy/jachwi-sunbae.service` | systemd 유닛 |
| `backend/deploy/scripts/` | 배포 훅 스크립트 |

## 빌드 검증

배포 빌드는 `clean bootJar -x test`로 실행 가능한 JAR를 만든다. GitHub Actions는 PR과 `main`·`develop` push에서 `clean build`를 실행한다. 두 브랜치는 [브랜치와 커밋](../../../docs/convention/branch-and-commit.md)의 보호 규칙에 따라 필수 검사를 통과하지 않으면 병합할 수 없으므로 CodePipeline이 받는 커밋은 이미 검증을 통과한 상태다. 현재 초기화된 백엔드에는 테스트 소스가 없지만 이후 테스트가 추가되어도 같은 경계를 유지한다.

## 배포 훅

| 훅 | 하는 일 |
| --- | --- |
| `ApplicationStop` | 서비스 중지를 요청한다. **직전 리비전의 스크립트가 실행되므로 첫 배포에는 실행되지 않는다.** 교체를 이 훅에 의존하지 않는다 |
| `BeforeInstall` | `/opt/jachwi-sunbae`를 비운다. 이 배포가 만들지 않은 파일이 남아 있으면 CodeDeploy가 실패한다 |
| `AfterInstall` | 환경변수 파일과 실행 사용자의 존재를 확인하고, 권한을 맞추고, systemd 유닛을 설치한다 |
| `ApplicationStart` | 서비스를 **재시작**한다. 실제 프로세스 교체를 보장하는 단계다 |
| `ValidateService` | `/actuator/health`가 `UP`이고 `/actuator/info`의 `build.commit`이 이번 배포 SHA와 같은지 확인한다. 실패하면 배포를 중단하고 최근 로그를 남긴다 |

## 왜 `start`가 아니라 `restart`인가

`ApplicationStart`는 `systemctl start`가 아니라 `systemctl restart`를 쓴다.

`start`는 서비스가 이미 `active`이면 아무 일도 하지 않는다. `ApplicationStop`이 어떤 이유로든 중지에 실패하면 옛 프로세스가 그대로 남고 새 jar는 실행되지 않는다. 그 상태에서 `ValidateService`가 health를 확인하면 **옛 프로세스가 응답해 배포가 성공으로 기록된다.** 실제로는 아무것도 바뀌지 않았는데 초록불이 뜬다.

실제로 이 일이 있었다. `stop.sh`가 `systemctl list-unit-files` 출력을 grep해 서비스 존재를 검사했는데 그 검사가 어긋나 중지를 건너뛰었고, 이어진 배포가 1초 만에 health를 통과했다.

`ApplicationStop`에 기대는 설계 자체가 옳지 않다. 이 훅은 **직전 리비전의 스크립트**로 실행되므로 첫 배포에서는 아예 실행되지 않고, 직전 리비전의 스크립트가 잘못돼 있으면 동작하지도 않는다. 프로세스 교체는 `restart`가 보장한다.

## 이번 리비전이 실행됐는지 확인하는 방법

`restart`는 알려진 프로세스 교체 실패를 막지만 실행 중인 프로세스의 정체를 증명하지는 않는다. 빌드와 검증은 같은 소스 SHA를 다음 두 곳에 넣는다.

- Gradle `buildInfo`의 `build.commit`: 실행 중인 애플리케이션이 `/actuator/info`로 응답한다.
- `deployment-revision.txt`: CodeDeploy가 이번 산출물과 함께 `/opt/jachwi-sunbae`에 배치한다.

`ValidateService`는 health가 `UP`이어도 두 SHA가 다르면 즉시 실패한다. 옛 프로세스가 응답하거나 다른 산출물이 배포된 경우를 성공으로 기록하지 않는다. `deployment-revision.txt`가 없거나 SHA 형식이 아니어도 실패한다.

## 애플리케이션 포트

**운영에서 애플리케이션은 80을 듣는다.** 로컬 기본값 8080과 다르다.

`project-app` 보안 그룹은 `project-lb`에서 오는 80과 443만 허용한다. 8080은 열려 있지 않고, 공용 보안 그룹이라 규칙을 추가하면 다른 팀 인스턴스에도 열린다. 그래서 규칙을 바꾸지 않고 애플리케이션을 80으로 옮겼다.

비루트 계정은 1024 미만 포트에 바인딩할 수 없으므로 systemd 유닛에서 `AmbientCapabilities=CAP_NET_BIND_SERVICE`를 준다. EC2 안에서 iptables로 80을 8080으로 넘기는 방법도 있지만, 그 규칙은 저장소에 남지 않고 재부팅 시 사라져 따로 영속화해야 한다.

포트를 바꾸면 세 곳을 함께 고친다. `application-prod.yml`의 `server.port`, `scripts/validate.sh`의 `HEALTH_URL`, ALB 대상 그룹의 포트다. **대상 그룹은 만든 뒤 포트를 바꿀 수 없다.**

## 서버에 있어야 하는 것

배포는 다음을 전제한다. 없으면 `AfterInstall`에서 멈춘다.

| 대상 | 내용 |
| --- | --- |
| `/etc/jachwi-sunbae/app.env` | 운영 환경변수. `0600`, 소유자 `root:root`. **환경마다 값이 다르다** |
| 사용자 `jachwi` | 애플리케이션 실행 계정. 로그인 셸이 없다 |
| 디렉터리 `/opt/jachwi-sunbae` | 배포 대상 |
| CodeDeploy 에이전트 | `systemctl status codedeploy-agent`가 `active` |

**환경변수 파일은 배포 산출물에 넣지 않는다.** CodeDeploy가 덮어쓰는 경로 밖에 두어 배포마다 값이 사라지지 않게 한다. systemd가 `EnvironmentFile`로 root 권한에서 읽은 뒤 `jachwi`로 내려가므로 애플리케이션 계정에 읽기 권한을 주지 않는다.

현재 초기화된 애플리케이션은 운영 비밀값을 사용하지 않는다. 다만 배포 훅과 systemd 유닛의 기존 계약을 유지하기 위해 환경변수 파일은 빈 파일로라도 둔다. 새 환경변수를 도입할 때 [환경변수](../guides/environment-variables.md)와 배포 환경을 함께 갱신한다.

`SPRING_PROFILES_ACTIVE`는 dev와 prod 모두 `prod`로 둔다. 이 프로필은 애플리케이션이 80 포트를 사용하게 한다.

## 빌드를 CodeBuild가 아니라 Commands로 하는 이유

별도 CodeBuild 프로젝트를 두지 않고 CodePipeline의 `Commands` 액션을 쓴다. 같은 계정의 다른 팀이 CodeBuild 경로에서 막혔기 때문이다. `codebuild-project` 서비스 role에 파이프라인 `SourceArtifact`를 읽을 `s3:GetObject` 권한이 없어 빌드 입력을 내려받지 못했고, 팀은 role 정책을 바꿀 수 없다.

`Commands`는 파이프라인 서비스 role을 그대로 쓰므로 이 문제를 겪지 않는다. 대신 빌드 정의가 저장소가 아니라 콘솔에 있다. 파이프라인 설정을 바꾸면 이 문서를 함께 고친다.

빌드 명령은 다음을 한다.

1. Corretto 21을 설치하고 `JAVA_HOME`을 잡는다. **관리형 환경의 기본 Java가 17일 수 있어 버전을 명시한다.**
2. Source 작업이 출력한 `CommitId`를 `SOURCE_COMMIT_ID`로 전달받아 40자리 Git SHA인지 확인한다.
3. 그 SHA를 `build.commit`으로 넣고 `clean bootJar -x test`로 실행 가능한 jar를 만든다.
4. 같은 SHA를 `deployment-revision.txt`에 기록한다.
5. `-plain.jar`가 아닌 jar를 `app.jar`로 복사한다.
6. `backend/deploy/`의 내용을 작업 디렉터리 루트로 옮긴다. **`appspec.yml`이 아티팩트 최상단에 없으면 CodeDeploy가 배포를 시작하지 못한다.**

Commands 액션은 GitHub 소스를 직접 받는 CodeBuild 프로젝트가 아니라 `SourceArtifact`를 입력으로 받는다. 이 구성에서는 `CODEBUILD_RESOLVED_SOURCE_VERSION`이 자동으로 제공되지 않으므로 Source 작업의 출력 변수를 Build 작업에 명시적으로 연결한다.

| 설정 위치 | 값 |
| --- | --- |
| Source 작업 변수 네임스페이스 | `SourceVariables` |
| Build 작업 환경 변수 이름 | `SOURCE_COMMIT_ID` |
| Build 작업 환경 변수 값 | `#{SourceVariables.CommitId}` |

빌드 명령에는 다음 검증과 파일 생성을 포함한다.

```bash
REVISION="${SOURCE_COMMIT_ID:?source revision is missing}"
printf '%s' "${REVISION}" | grep -Eq '^[0-9a-f]{40}$'
SOURCE_COMMIT_ID="${REVISION}" ./backend/gradlew -p backend --no-daemon --max-workers=1 clean bootJar -x test
printf '%s\n' "${REVISION}" > deployment-revision.txt
```

출력 아티팩트 `BuildArtifact`에는 `app.jar`, `deployment-revision.txt`, `appspec.yml`, `jachwi-sunbae.service`, `scripts/**/*`가 들어간다. 이 목록을 비워두면 Deploy 단계의 입력이 저장소 원본으로 잡혀 배포가 실패한다.

## 로그 확인

```bash
sudo journalctl -u jachwi-sunbae.service -f
sudo systemctl status jachwi-sunbae.service
```

배포 자체가 실패했다면 EC2의 `/opt/codedeploy-agent/deployment-root/deployment-logs/`를 함께 본다.

배포 결과는 관련 GitHub 이슈 또는 PR에 기록한다.
