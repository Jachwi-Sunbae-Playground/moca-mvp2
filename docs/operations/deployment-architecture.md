# 배포 아키텍처 설계

- 상태: 제안
- 최초 작성일: 2026-08-13
- 참여자: 자취선배 백엔드 팀
- 문서 성격: 파생
- 대조 대상: 우테코 인프라 안내(Notion), 실제 AWS 리소스 구성, [배포](../../backend/docs/operations/deployment.md)
- 갱신 정책: 리소스를 실제로 구성하면 이 문서의 값을 실구성과 맞춘다. 팀이 승인하면 중심 결정은 ADR로 승격하고 이 문서는 구성 참조로 남긴다

이 문서는 [배포](../../backend/docs/operations/deployment.md)와 [롤백](../../backend/docs/operations/rollback.md)이 `미정`으로 비워 둔 배포 대상과 플랫폼을 채우기 위한 설계 초안이다. 아직 리소스를 만들지 않았으므로 계획 문서로 두고, 실제 구성 뒤 값을 실구성과 맞춘다.

실행 체크리스트는 이 문서에 두지 않는다. 단계별 작업과 확인 항목은 배포 환경 구축 이슈와 그 하위 이슈에서 관리한다.

## 1. 목표와 제약

### 목표

외부 사용자가 접근할 수 있도록 백엔드와 프론트엔드를 배포하고([지정 요구사항 2](../../backend/docs/requirements/02-deploy-and-observe.md)), `main` 병합에서 검증·배포까지 자동으로 이어지게 한다. 프론트엔드는 React 19 + TypeScript + Webpack 5(Node 22.23.1)로 이미 개발되어 있으므로 이번 설계의 실행 범위는 백엔드와 프론트엔드 배포, 진입 계층을 모두 포함한다.

[시스템 개요](../../backend/docs/architecture/system-overview.md)에는 프론트엔드가 "개발 예정"으로 적혀 있으나 실제 `frontend/`에는 코드가 있다. 배포 착수와 함께 해당 문서의 표기를 실제 상태로 맞춘다.

### 제약

우테코 AWS 인프라 안내에서 이번 설계를 좌우하는 제약은 다음과 같다.

- **예산**: 8월 $50, 9월 $60, 10월 이후 $70. 초과 시 사용 중인 리소스를 종료·삭제한다.
- **IAM Role·액세스 키 생성 불가**: 보안 정책상 팀이 IAM Role이나 액세스 키를 새로 만들 수 없다. 따라서 GitHub Actions가 액세스 키로 AWS에 직접 배포하는 흔한 방식을 쓰지 않고, 제공된 service role과 EC2 인스턴스 role만 사용한다.
- **네트워크 고정**: VPC·서브넷·보안 그룹은 이미 만들어진 것을 지정해 사용한다.
- **삭제 권한 제한**: 대부분의 삭제 권한이 없다. 리소스 정리가 필요하면 `#8기-기술-검토`에 문의한다.
- **태그 필수**: 모든 리소스에 `Service=techcourse`, `Role=techcourse-etc`, `ProjectTeam=<팀 서비스 영문명>`을 설정한다. 없으면 리소스를 종료·삭제한다.
- **EC2 타입 상한**: `t4g.medium` 이하.

## 2. 확정한 결정 요약

| 항목 | 결정 | 핵심 이유 |
| --- | --- | --- |
| 컴퓨트 | EC2 1대, `t4g.small`, `project-app` 서브넷 | 백엔드 전용. 예산 안에서 JVM에 필요한 메모리(2GB) 확보 |
| 데이터베이스 | RDS MySQL, `db.t4g.micro`, `project-storage` 서브넷 | 자동 백업·스냅샷이 Flyway 롤백 절차의 "검증된 백업 복구" 전제와 맞음 |
| 사진 저장소 | S3 `techcourse-project-2026` 팀 폴더 | 로컬 MinIO와 같은 S3 API 경계([ADR-0006](../../backend/docs/adr/0006-use-private-s3-compatible-photo-storage.md)). EC2 인스턴스 role로 접근 |
| 프론트 서빙 | S3 + CloudFront | 정적 SPA(React 19/Webpack). CDN 캐싱·백엔드와 분리. 환경변수는 빌드 타임 주입이라 운영 값으로 재빌드 필요 |
| 진입·HTTPS | ALB + WAF(`techcourse-project-waf`) + ACM | LB에 WAF 연결이 필수. ACM 퍼블릭 인증서는 무료 |
| 도메인 | 가비아에서 구매 | DNS 검증과 레코드는 가비아 DNS에서 설정 |
| 배포 자동화 | CodePipeline + CodeBuild + CodeDeploy | 액세스 키 없이 제공된 service role로 배포 |
| 비밀 관리 | SSM Parameter Store(SecureString) + EC2 `ec2-project` role | 액세스 키 없이 운영 비밀을 주입 |

EC2 배치 서브넷은 `project-app`의 인터넷 egress 여부에 따라 달라진다. [4.1 네트워크](#41-네트워크)를 참고한다.

## 3. 전체 구성

```text
사용자
  │
  ├─ (가비아 도메인 DNS)
  │
  ├─ www.<도메인>  → CloudFront ─ S3(프론트 정적 파일, React 19 SPA)
  │
  └─ api.<도메인>  → ALB(project-lb 서브넷) ─ WAF(techcourse-project-waf) ─ ACM(HTTPS)
                        │
                        ↓ HTTP
                     EC2(project-app 서브넷, t4g.small)
                     Spring Boot, prod 프로필
                        ├─ JDBC ─ RDS MySQL(project-storage 서브넷)
                        ├─ S3 API ─ techcourse-project-2026 버킷(사진)  ← EC2 ec2-project role
                        ├─ SSM ─ Parameter Store(운영 비밀)             ← EC2 ec2-project role
                        └─ HTTPS ─ Google token endpoint·JWK
```

배포 경로(코드 → 서비스)는 애플리케이션 트래픽과 분리된다.

```text
GitHub(main 병합)
  → CodePipeline(소스: GitHub 버전 1)
    → CodeBuild(codebuild-project role) ─ 빌드·테스트 ─ 산출물 → S3 techcourse-project-2026-artifacts
    → CodeDeploy(codedeploy-project role) ─ EC2(ec2-project role, CodeDeploy 에이전트)에 배포
    → 배포 후 Actuator health·smoke test 확인
```

## 4. 구성 요소별 설계

### 4.1 네트워크

- **VPC**: `TECHCOURSE-PROJECT`(`vpc-004e154d9f1f3f5cd`).
- **EC2 서브넷**: LB 뒤에 두므로 `project-app-a`, `project-app-b`. 보안 그룹 `project-app`.
- **ALB 서브넷**: `project-lb-a`, `project-lb-b`. 보안 그룹 `project-lb`.
- **RDS 서브넷**: 서브넷 그룹 `project-rds-subnet-group`(`project-storage-a/b`). 보안 그룹 `project-db`.
- 보안 그룹 원칙: ALB는 외부에서 443만 받고, EC2 애플리케이션 포트는 `project-lb`에서 오는 트래픽만 허용한다. RDS 3306은 `project-app`에서 오는 트래픽만 허용한다.

**EC2 배치는 인터넷 egress 확인 결과에 달려 있다.** 리소스를 만들기 전에 `project-app` 서브넷의 라우트 테이블에서 `0.0.0.0/0` 라우트를 확인한다.

| 확인 결과 | EC2 배치 |
| --- | --- |
| `nat-...`(NAT 게이트웨이)가 있다 | `project-app`(사설)에 둔다 |
| NAT가 없다 | `project-public` 서브넷 + 퍼블릭 IP로 띄우고 보안 그룹으로 잠근다 |

NAT 게이트웨이를 새로 만드는 선택지는 월 약 $32로 예산을 초과하므로 두지 않는다. 이 확인을 건너뛰고 사설 서브넷에 EC2를 만들면 애플리케이션은 기동하지만 Google 토큰 엔드포인트와 SSM에 나가지 못해 **로그인만 실패한다.** 원인을 찾기 어려운 종류의 실패다.

### 4.2 컴퓨트 (EC2)

- 타입 `t4g.small`(ARM, 2GB RAM)로 시작한다. `t4g.micro`(1GB)는 JVM에 빠듯해 최후의 축소 카드로만 둔다.
- AMI는 ARM 아키텍처(arm64)를 사용한다. `t4g`는 ARM이므로 x86 AMI를 고르면 기동하지 않거나 CodeDeploy 에이전트가 붙지 않는다.
- 인스턴스에 IAM role `ec2-project`를 연결한다. 이 role로 S3(사진)·SSM(비밀)·CloudWatch(로그)·CodeDeploy 산출물 접근을 액세스 키 없이 수행한다.
- CodeDeploy 에이전트와 애플리케이션 실행 런타임(JDK 21)을 설치한다.
- 태그 3종을 설정한다.

### 4.3 데이터베이스 (RDS)

- 엔진 MySQL 8.4 계열, `db.t4g.micro`, 스토리지 gp3 20GB로 시작한다.
- 자동 백업(보존 기간 설정)과 수동 스냅샷을 사용한다. 이는 [롤백](../../backend/docs/operations/rollback.md)이 요구하는 "검증된 백업을 격리된 대상에 복구"를 실제로 가능하게 하는 근거다.
- 퍼블릭 액세스를 끄고 `project-storage` 서브넷에 둔다. `project-app`에서만 접근한다.
- 시간대는 UTC로 둔다([시스템 개요](../../backend/docs/architecture/system-overview.md)의 UTC 기준과 일치).

### 4.4 사진 저장소 (S3)

- 운영 버킷 `techcourse-project-2026`의 **팀 폴더** 아래에 사진 객체를 둔다. 빌드 산출물은 별도 버킷(`techcourse-project-2026-artifacts`)을 사용한다.
- 로컬 MinIO와 운영 S3는 같은 애플리케이션 경계(`PhotoStorage`)를 쓴다([ADR-0006](../../backend/docs/adr/0006-use-private-s3-compatible-photo-storage.md)). 운영 전환에서 바뀌는 것은 **자격증명 주입 방식**이다. 로컬은 정적 키(MinIO 예시 값)를 쓰지만 운영은 정적 키를 두지 않고 EC2 `ec2-project` role로 접근한다.
- 버킷은 비공개를 유지하고, 사진 본문은 지금처럼 인증 백엔드가 스트리밍한다.

### 4.5 프론트엔드 (S3 + CloudFront)

- 실제 구성은 React 19 + TypeScript + Webpack 5, Node 22.23.1(`.nvmrc`), 라우팅은 react-router v7이다. 빌드는 `npm run build`(`webpack --mode production`)로 `dist/`를 만든다.
- 정적 SPA이므로 EC2가 아니라 S3에 올리고 CloudFront로 서빙한다. 프론트엔드 자율 요구사항의 `Cache Busting`·`CDN Cache Invalidation`·`contenthash`가 이 구조를 전제로 한다.
- CloudFront OAC는 인프라 안내가 지정한 `techcourse-project-2026.s3.ap-northeast-2.amazonaws.com`을 origin으로 사용한다.
- 캐시는 안내에 따라 Policy를 새로 만들지 않고 **레거시 캐시 설정(Legacy Cache Settings)** 을 사용한다.
- **SPA 폴백**: react-router 클라이언트 라우팅이므로 CloudFront에서 403·404 응답을 `/index.html`(200)로 매핑해 새로고침·딥링크가 깨지지 않게 한다. Google 콜백 경로 `/oauth/google/callback`도 프론트 라우트다.
- **환경변수는 빌드 타임에 주입된다.** `webpack.config.js`의 `DefinePlugin`이 `API_BASE_URL`·`GOOGLE_CLIENT_ID`·`GOOGLE_REDIRECT_URI`를 번들에 박아넣는다. 런타임 설정이 아니므로 운영 배포는 CodeBuild가 운영 값(`API_BASE_URL=https://api.<도메인>`, `GOOGLE_REDIRECT_URI=https://www.<도메인>/oauth/google/callback`)으로 **다시 빌드**해야 한다. 이 값들은 SSM Parameter Store 또는 파이프라인 환경변수로 CodeBuild에 전달한다.
- 현재 `webpack.config.js`의 `output`에 `[contenthash]` 파일명이 없어 캐시 무효화가 파일명 기반으로 동작하지 않는다. 배포 자동화 시 `contenthash`를 도입하거나 매 배포 CloudFront 무효화를 거는 방식 중 하나를 택한다.

### 4.6 진입 계층 (ALB + WAF + ACM)

- ALB를 `project-lb` 서브넷에 두고 443 HTTPS 리스너에 ACM 인증서를 붙인다. 80은 443으로 리다이렉트한다.
- **WAF 연결은 필수다.** 공용 WAF `techcourse-project-waf`를 ALB에 연결한다. 연결하지 않으면 요청을 받지 못한다.
- **ACM 인증서는 리전에 주의한다.** ALB용 인증서는 서울(`ap-northeast-2`)에서 발급한다. CloudFront용 인증서는 **버지니아 북부(`us-east-1`)에서 발급해야** 한다. 두 인증서 모두 DNS 검증을 가비아 DNS에 CNAME으로 추가한다.

### 4.7 도메인·DNS (가비아)

- 가비아에서 도메인을 구매하고, 가비아 DNS에서 레코드를 관리한다.
- `api.<도메인>` → ALB DNS 이름으로 향하는 레코드, `www.<도메인>` → CloudFront 배포 도메인으로 향하는 레코드를 둔다.
- 가비아는 apex(`@`)에 CNAME을 넣을 수 없으므로 서브도메인을 사용한다.
- ACM DNS 검증용 CNAME과 서비스 레코드를 함께 관리한다.
- 운영 도메인이 정해지면 애플리케이션의 `CORS_ALLOWED_ORIGINS`와 `GOOGLE_OAUTH_ALLOWED_REDIRECT_URIS`를 운영 값으로 바꾸고, Google OAuth 콘솔의 허용 redirect URI에도 등록한다. wildcard는 쓰지 않는다.

### 4.8 비밀·환경변수

- 운영 프로필 `prod`를 신설한다. 로컬 기본값([환경변수](../../backend/docs/guides/environment-variables.md))과 분리한다.
- 운영 비밀(`DB_PASSWORD`, `JWT_SECRET_BASE64`, `GOOGLE_OAUTH_CLIENT_SECRET` 등)은 SSM Parameter Store의 `SecureString`에 두고, EC2 `ec2-project` role로 기동 시 읽어 주입한다. 액세스 키를 EC2에 두지 않는다.
- 실제 비밀은 저장소·문서·`.env.example`에 커밋하지 않는다는 원칙을 그대로 유지한다.

## 5. 배포 자동화 파이프라인

액세스 키를 만들 수 없으므로 **AWS 네이티브 파이프라인**으로 구성한다. 모든 단계가 제공된 service role로 동작한다.

1. **소스**: CodePipeline 소스 공급자는 GitHub(버전 1). `main` 병합을 트리거로 한다.
2. **빌드·검증**: CodeBuild(service role `codebuild-project`)가 Gradle 빌드와 테스트를 실행한다. 산출물은 `techcourse-project-2026-artifacts`에 저장하고, 로그는 CloudWatch 그룹 `/aws/codebuild/project-2026`을 사용한다.
3. **배포**: CodeDeploy(service role `codedeploy-project`)가 EC2에 배포한다. EC2에는 CodeDeploy 에이전트가 있고 `ec2-project` role로 산출물을 받는다. `appspec.yml`과 배포 훅 스크립트로 기동·전환을 정의한다.
4. **배포 후 검증**: 배포 훅에서 Actuator health(`{"status":"UP"}`)와 핵심 smoke test(로그인·매물 조회 등 대표 흐름)를 확인한다. 실패하면 배포를 중단한다.

프론트엔드 파이프라인도 같은 원칙(액세스 키 없이 service role) 위에 구성한다. CodeBuild(Node 22.23.1)가 운영 환경변수로 `npm ci && npm run build`를 수행해 `dist/`를 만들고, 결과를 S3 `techcourse-project-2026` 팀 폴더에 동기화한 뒤 CloudFront 캐시를 무효화한다. 백엔드와 별도 파이프라인으로 두어 한쪽 실패가 다른 쪽 배포를 막지 않게 한다.

PR 검증은 기존 GitHub Actions(`.github/workflows/backend-ci.yml`)가 맡고, 배포용 빌드는 CodeBuild가 맡는다. 두 경계를 구분해 유지한다.

## 6. 데이터베이스 변경이 포함된 배포

절차는 [배포](../../backend/docs/operations/deployment.md), [데이터베이스 마이그레이션 가이드](../../backend/docs/guides/database-migrations.md), [ADR-0007](../../backend/docs/adr/0007-use-flyway-for-database-migrations.md)이 정본이다. 여기서는 이번 배포 환경에서 달라지는 점만 적는다.

- **첫 배포는 baseline 대상이 아니다.** [배포](../../backend/docs/operations/deployment.md)의 baseline 단계는 pre-Flyway v1.0 DB를 전제한다. 새로 만드는 RDS는 빈 DB이므로 Flyway가 V1부터 최신까지 그대로 적용한다. `baseline-on-migrate`는 상시 설정으로 두지 않는다.
- **대상 DB 교차 확인의 기준이 생긴다.** 지금까지 로컬 MySQL뿐이었으나 운영 RDS가 추가되므로, 마이그레이션 전 접속 대상이 의도한 RDS인지 확인한다.
- 이후 DB 변경 배포는 정본 문서의 절차를 그대로 따른다.

## 7. 롤백

- 애플리케이션 롤백은 CodeDeploy의 직전 정상 리비전으로 되돌린다. 판단 기준과 승인자는 [롤백](../../backend/docs/operations/rollback.md)을 따른다.
- 데이터베이스는 자동 down migration을 두지 않는다. 데이터 유입 시점에 따라 검증된 백업 복구 또는 후속 순방향 마이그레이션 중 안전한 방법을 선택한다.

## 8. 비용 추정

서울 리전 기준 월 환산(대략)이다. 실제 8월은 배포 시점 이후 일수만 과금되므로 아래보다 낮다.

| 항목 | 사양 | 월 환산(대략) |
| --- | --- | --- |
| EC2 | `t4g.small` 1대 | ~$15 |
| RDS | `db.t4g.micro` + gp3 20GB | ~$15 |
| ALB | 기동 시간요금 + 소량 LCU | ~$17 |
| WAF | 공용 `techcourse-project-waf` | $0 또는 ~$6~10(주체 확인 중) |
| ACM | 퍼블릭 인증서 | $0 |
| S3·CloudFront·전송량 | 소량 | ~$2 |
| **합계** | | **~$49(WAF 팀 부담 시 ~$55~59)** |

- ACM은 무료다.
- WAF가 우테코 공용 부담이면 8월·9월 모두 여유가 있다. 팀 부담이면 9월($60 한도)부터 CloudFront 앞단으로 WAF를 옮기는 변형이나 EC2 축소를 검토한다.
- 8월은 남은 일수(약 15일)만 과금되므로 한도 초과 위험이 낮다. 예산 판단의 기준 달은 처음으로 한 달을 꽉 채우는 9월이다.
- **WAF 비용 주체를 확인하기 전에는 ALB를 만들지 않는다.** 팀 부담이면 9월 한도에 여유가 거의 없어 진입 계층 구성을 다시 판단해야 한다.

## 9. 미결 사항

| 항목 | 상태 | 필요한 확인 |
| --- | --- | --- |
| `project-app` 인터넷 egress | 확인 필요 | NAT 게이트웨이 유무에 따라 EC2 배치 서브넷이 갈린다. 리소스 생성 전에 확인한다 |
| WAF 비용 주체 | 확인 중 | `#8기-기술-검토`에 공용 WAF 연결 시 비용 주체 문의 |
| 제공 role 권한 범위 | 확인 필요 | `codebuild-project`·`codedeploy-project`·`ec2-project`가 S3·CloudFront·SSM에 필요한 권한을 포함하는지 확인. 부족하면 `#8기-기술-검토` 문의 |
| 프론트 운영 env·재빌드 | 반영 필요 | 빌드 타임 주입이므로 CodeBuild에 운영 `API_BASE_URL`·`GOOGLE_REDIRECT_URI` 전달. Google OAuth 콘솔·백엔드 허용 목록에도 운영 redirect URI 등록 |
| 프론트 캐시 무효화 | 결정 필요 | `contenthash` 도입 또는 매 배포 CloudFront 무효화 중 택1 |
| 시스템 개요 문서 | 갱신 필요 | "프론트 개발 예정" 표기를 실제 상태로 수정 |

## 10. 검토한 대안

| 결정 | 채택 | 검토한 대안 | 채택하지 않은 이유 |
| --- | --- | --- | --- |
| 배포 자동화 | CodePipeline+CodeBuild+CodeDeploy | GitHub Actions self-hosted runner를 EC2에 설치 | 셋업은 더 단순하나, 팀이 AWS 네이티브 CI/CD 학습을 자율 요구사항으로 가져갈 수 있어 학습 가치가 큰 쪽을 택함. 러너 방식은 축소 대안으로 유지 |
| 데이터베이스 | RDS MySQL | EC2에 MySQL 직접 설치 | 비용은 낮으나 백업·복구·운영 부담이 크고, 롤백 절차의 백업 복구 전제와 맞지 않음 |
| 프론트 서빙 | S3+CloudFront | EC2에 nginx로 함께 서빙 | 정적 SPA에 CDN 캐싱 이점이 크고 백엔드와 장애가 분리됨. 프론트엔드 캐시 요구사항과도 맞음 |
| 진입 계층 | ALB+WAF+ACM | EC2에 직접 도메인·HTTPS(certbot) | ACM·WAF는 EC2에 직접 붙지 않고, "요청 수신에 WAF 필요" 요건을 EC2 단독으로 충족할 수 없음. 비용 압박 시 CloudFront 앞단으로 대체 검토 |
