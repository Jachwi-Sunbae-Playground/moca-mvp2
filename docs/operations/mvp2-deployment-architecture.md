# MVP2 배포 아키텍처

- 상태: 운영 인프라 구성·최초 배포 검증 완료
- 결정일: 2026-08-22
- 문서 성격: 파생
- 대조 대상: `deploy/`, `.github/workflows/deploy-production.yml`, 실제 개인 AWS 구성

MVP2 기능은 로컬 검증을 마쳤다. 외부 사용자가 확인할 수 있는 `jachwisunbae.shop`을 위해 도메인, EC2, 비공개 S3, GitHub OIDC와 SSM을 구성했고 최초 배포와 사진 업로드를 검증했다. 인증은 외부 계정 없이 닉네임과 선택 비밀번호를 사용한다. 중심 배포 결정은 [ADR-0010](../../backend/docs/adr/0010-prepare-single-ec2-deployment.md), 기존 DB 보강은 [ADR-0011](../../backend/docs/adr/0011-apply-idempotent-database-upgrades.md)에 기록한다.

## 전체 구성

```text
사용자
  ↓ HTTPS
Caddy :80/:443
  ├─ React 정적 파일 + SPA fallback
  └─ /api/* → Spring Boot 127.0.0.1:8080
                  ├─ MySQL 8.4 127.0.0.1:3306
                  └─ 비공개 S3 사진 버킷

GitHub Actions 수동 실행
  → OIDC로 AWS 역할 위임
  → 릴리스 S3 업로드
  → SSM Run Command
  → EC2 릴리스 교체·health 확인
```

## 구성 요소

| 구성 요소 | 결정 | 외부 공개 |
| --- | --- | --- |
| EC2 | 4GB 이상 메모리의 인스턴스 1대, 고정 공인 IP | 80·443만 허용 |
| Caddy | HTTPS 인증서 자동 관리, 정적 SPA와 `/api` 라우팅 | 공개 |
| Spring Boot | systemd, `prod` 프로필, 8080 | 비공개 loopback |
| MySQL | Docker Compose, 영속 EBS 볼륨, 3306 | 비공개 loopback |
| 사진 S3 | private bucket, EC2 instance role로 접근 | 직접 공개하지 않음 |
| 릴리스 S3 | Actions가 산출물 업로드, EC2 role이 읽음 | 비공개 |
| SSM | SSH 대신 배포 명령 실행 | 인바운드 포트 없음 |

EC2에서는 소스 빌드를 실행하지 않는다. 빌드는 GitHub Actions가 하고 EC2는 압축된 릴리스만 내려받는다. MySQL 포트와 Spring Boot 포트는 `127.0.0.1`에만 열며 보안 그룹에도 추가하지 않는다.

## 배포 정책

- `main` push와 PR 병합은 배포를 실행하지 않는다.
- `Deploy production` 워크플로를 `main`에서 수동 실행하고 확인값 `DEPLOY`를 입력해야 한다.
- GitHub `production` Environment의 변수와 필요하면 수동 승인 규칙을 사용한다.
- PR에서 통과한 테스트를 배포 시 다시 실행하지 않고 백엔드 JAR와 프론트 정적 파일만 빌드한다.
- 장기 AWS Access Key와 SSH 개인키를 GitHub에 저장하지 않는다.
- SSM 명령에는 비밀값을 넣지 않는다. 런타임 비밀값은 EC2의 `/etc/moca/app.env`에 `root:root`, `0600`으로 둔다.
- Actions는 SSM `commands` 매개변수를 JSON 문자열 배열로 직렬화한다. AWS CLI shorthand에 셀 따옴표를 중첩하지 않는다.
- SSM 대기가 실패하면 `GetCommandInvocation`을 출력해 EC2 배포 스크립트의 표준 출력과 표준 오류를 Actions 로그에 남긴다.
- 배포 후 `127.0.0.1:8080/actuator/health`가 실패하면 애플리케이션 심볼릭 링크를 직전 릴리스로 되돌린다.
- DB 스키마와 데이터는 애플리케이션 롤백 대상이 아니다.

## GitHub Environment 변수

| 변수 | 용도 |
| --- | --- |
| `AWS_DEPLOY_ROLE_ARN` | GitHub OIDC가 위임할 최소 권한 역할 |
| `AWS_REGION` | EC2·SSM·릴리스 S3 리전 |
| `EC2_INSTANCE_ID` | 배포 대상 단일 인스턴스 |
| `RELEASE_BUCKET` | 비공개 릴리스 아카이브 버킷 |
| `MOCA_DOMAIN` | 공개 서비스 도메인 |
| `KAKAO_MAP_JAVASCRIPT_KEY` | 등록 도메인에서 사용하는 공개 Kakao JavaScript 키 |

애플리케이션의 JWT, 닉네임 비밀번호, Kakao REST 키, 공공데이터포털 키, DB 비밀번호는 GitHub 변수에 두지 않는다. 닉네임 원문 비밀번호는 서버 환경변수에도 저장하지 않고 요청 시 BCrypt로 검증한다. EC2 instance role에는 사진 버킷의 `GetObject`, `PutObject`, `DeleteObject`와 필요한 버킷 조회 권한만 추가한다.

## 최초 배포 전 준비

다음 항목을 모두 준비하기 전에는 워크플로를 실행하지 않는다.

1. 도메인과 EC2 고정 공인 IP를 연결한다.
2. EC2 보안 그룹에서 80·443만 외부에 열고 SSH와 3306·8080은 열지 않는다.
3. Java 21, Docker, Caddy, AWS CLI와 SSM Agent를 설치한다.
4. `moca` 시스템 사용자를 만들고 `/opt/moca/releases`, `/etc/moca`를 준비한다.
5. `deploy/moca-backend.service`를 systemd에 등록한다.
6. `deploy/Caddyfile.example`의 도메인을 바꿔 Caddy 설정으로 등록한다.
7. `deploy/scripts/deploy-release.sh`를 `/usr/local/bin/moca-deploy`로 설치한다.
8. `/etc/moca/app.env`에 운영 환경변수를 만들고 `0600` 권한을 적용한다.
9. 사진·릴리스 S3 버킷, EC2 role, GitHub OIDC role과 SSM 최소 권한을 만든다.
10. GitHub `production` Environment 변수를 등록한다.

첫 릴리스 압축에는 MySQL 초기화 SQL도 포함된다. 배포 스크립트가 이를 안정된 `/opt/moca/shared/db-init`에 복사한 뒤 `deploy/compose.yaml`의 MySQL을 시작하고 healthy 상태를 기다린다. 빈 DB를 처음 시작할 때만 init 스키마가 적용되며 실제 데이터가 생긴 뒤에는 볼륨을 초기화하지 않는다. 기존 DB의 additive 변경은 JAR에 포함된 번호순 멱등 upgrade SQL이 애플리케이션 요청 수신 전에 적용한다.

## 첫 배포 검증

- 공개 도메인이 HTTPS로 열리고 인증서 갱신 구성이 정상이다.
- `/properties` 직접 진입과 새로고침이 SPA로 응답한다.
- 공개 `/api` 요청이 Spring Boot로 전달된다.
- EC2 내부 health가 `UP`이고 systemd 재시작 뒤에도 기동한다.
- 외부에서 3306·8080과 SSH에 접근할 수 없다.
- 신규 공유·보호 닉네임과 기존 회원에서 변환된 닉네임으로 시작할 수 있고 CORS가 공개 Origin에서 동작한다.
- 인증 없는 S3 객체 직접 조회가 차단된다.
- 실패 릴리스로 애플리케이션 롤백을 한 번 리허설한다.
- MySQL 백업을 별도 볼륨 또는 S3에서 복원하는 절차를 검증한다.

## 백업과 확장 기준

최소 구성이라도 MySQL 논리 백업을 매일 생성하고 EC2와 수명이 분리된 비공개 S3에 보관한다. 실제 주기, 보관 기간과 복원 명령은 첫 배포 Issue에서 확정하고 복원 리허설 전에는 공개 사용자를 받지 않는다.

다음 상황에서는 구성 요소를 분리한다.

| 신호 | 우선 검토 |
| --- | --- |
| 데이터 손실 허용 범위가 일일 백업보다 짧음 | RDS와 자동 백업·시점 복구 |
| JVM과 MySQL 메모리 경쟁 반복 | DB 분리 또는 인스턴스 확장 |
| 단일 장애 허용 불가 | 다중 인스턴스·로드밸런서 |
| 정적 전송량 증가 | S3·CloudFront 프론트 분리 |
| 배포 중단이 사용자에게 문제 | 별도 프로세스·블루그린 배포 |
