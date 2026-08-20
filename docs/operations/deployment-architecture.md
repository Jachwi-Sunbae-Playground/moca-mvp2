# 배포 아키텍처 설계

- 상태: 구성 완료
- 최초 작성일: 2026-08-13
- 참여자: 자취선배 백엔드 팀
- 문서 성격: 파생
- 대조 대상: 우테코 인프라 안내(Notion), 실제 AWS 리소스 구성, [배포](../../backend/docs/operations/deployment.md)
- 갱신 정책: 이 문서의 값을 실구성과 맞춰 유지한다. 중심 결정은 [ADR-0008](../../backend/docs/adr/0008-deploy-with-aws-native-pipeline.md)로 승격했고 이 문서는 구성 참조로 남는다

이 문서는 [배포](../../backend/docs/operations/deployment.md)와 [롤백](../../backend/docs/operations/rollback.md)이 `미정`으로 비워 둔 배포 대상과 플랫폼을 채우기 위해 시작했다. 2026-08-13부터 08-15까지 실제로 구성했고 아래 주소에서 동작한다.

| 환경 | 브랜치 | 프론트엔드 | 백엔드 |
| --- | --- | --- | --- |
| prod | `main` | `https://www.jachwi-sunbae.kr` | `https://api.jachwi-sunbae.kr` |
| dev | `develop` | `https://dev.jachwi-sunbae.kr` | `https://dev-api.jachwi-sunbae.kr` |

**왜 이렇게 구성했는지는 [ADR-0008](../../backend/docs/adr/0008-deploy-with-aws-native-pipeline.md)에 있다.** 이 문서는 구성 값과 절차를 담고, ADR은 결정의 맥락과 검토한 대안을 담는다. 절차는 [배포](../../backend/docs/operations/deployment.md)와 [프론트엔드 배포](../../frontend/docs/deployment.md)를 따른다.

실행 체크리스트는 이 문서에 두지 않는다. 단계별 작업과 확인 항목은 배포 환경 구축 이슈와 그 하위 이슈에서 관리한다.

## 1. 목표와 제약

### 목표

외부 사용자가 접근할 수 있도록 백엔드와 프론트엔드를 배포하고, `main` 병합에서 검증·배포까지 자동으로 이어지게 한다. 프론트엔드는 React 19 + TypeScript + Webpack 5(Node 22.23.1)로 이미 개발되어 있으므로 이번 설계의 실행 범위는 백엔드와 프론트엔드 배포, 진입 계층을 모두 포함한다.

[시스템 개요](../../backend/docs/architecture/system-overview.md)에는 프론트엔드가 "개발 예정"으로 적혀 있으나 실제 `frontend/`에는 코드가 있다. 배포 착수와 함께 해당 문서의 표기를 실제 상태로 맞춘다.

### 제약

우테코 AWS 인프라 안내에서 이번 설계를 좌우하는 제약은 다음과 같다.

- **예산**: 8월 $50, 9월 $60, 10월 이후 $70. 초과 시 사용 중인 리소스를 종료·삭제한다.
- **IAM Role·액세스 키 생성 불가**: 보안 정책상 팀이 IAM Role이나 액세스 키를 새로 만들 수 없다. 따라서 GitHub Actions가 액세스 키로 AWS에 직접 배포하는 흔한 방식을 쓰지 않고, 제공된 service role과 EC2 인스턴스 role만 사용한다.
- **네트워크 고정**: VPC·서브넷·보안 그룹은 이미 만들어진 것을 지정해 사용한다.
- **삭제 권한 제한**: 대부분의 삭제 권한이 없다. 리소스 정리가 필요하면 `#8기-기술-검토`에 문의한다.
- **태그 필수**: 모든 리소스에 `Service=techcourse`, `Role=techcourse-etc`, `ProjectTeam=jachwi-sunbae`를 설정한다. 없으면 리소스를 종료·삭제한다. 비용이 $0인 리소스(ACM 인증서 등)도 예외가 아니다.
- **EC2 타입 상한**: `t4g.medium` 이하.

## 2. 확정한 결정 요약

| 항목 | 결정 | 핵심 이유 |
| --- | --- | --- |
| 컴퓨트 | EC2 1대, `t4g.small`, `project-app` 서브넷 | 백엔드 전용. 예산 안에서 JVM에 필요한 메모리(2GB) 확보 |
| 데이터베이스 | RDS MySQL, `db.t4g.micro`, `project-storage` 서브넷 | 자동 백업과 스냅샷으로 데이터 복구 기반을 제공함 |
| 사진 저장소 | S3 `techcourse-project-2026` 팀 폴더 | 로컬 MinIO와 같은 S3 API 경계([ADR-0006](../../backend/docs/adr/0006-use-private-s3-compatible-photo-storage.md)). EC2 인스턴스 role로 접근 |
| 프론트 서빙 | S3 + CloudFront | 정적 SPA(React 19/Webpack). CDN 캐싱·백엔드와 분리. 환경변수는 빌드 타임 주입이라 운영 값으로 재빌드 필요 |
| 진입·HTTPS | ALB + WAF(`techcourse-project-waf`) + ACM | LB에 WAF 연결이 필수. WAF 요금은 팀 예산에서 제외되고 ACM 퍼블릭 인증서는 무료 |
| 도메인 | 가비아에서 구매한 `jachwi-sunbae.kr` | DNS 검증과 레코드는 가비아 DNS에서 설정 |
| 배포 자동화 | CodePipeline `Commands` + CodeDeploy | 액세스 키 없이 제공된 service role로 배포 |
| 비밀 관리 | EC2 로컬 설정 파일(`0600`) + CodeDeploy 훅 | 액세스 키를 만들 수 없고, SSM Parameter Store는 계정 전체가 한 범위라 팀 간 격리가 되지 않음 |

확정된 식별자는 다음과 같다. 팀 서비스 영문명은 도메인과 맞춰 `jachwi-sunbae`로 두고 태그·S3 폴더명·설정 경로에 일관되게 쓴다.

| 항목 | 값 |
| --- | --- |
| AWS 계정 | `843255971531` |
| 팀 서비스 영문명 | `jachwi-sunbae` |
| 도메인 | `jachwi-sunbae.kr` |
| 백엔드 | `api.jachwi-sunbae.kr` |
| 프론트엔드 | `www.jachwi-sunbae.kr` |

## 3. 전체 구성

```text
사용자
  │
  ├─ (가비아 도메인 DNS)
  │
  ├─ www.jachwi-sunbae.kr  → CloudFront ─ S3(프론트 정적 파일, React 19 SPA)
  │
  └─ api.jachwi-sunbae.kr  → ALB(project-lb 서브넷) ─ WAF(techcourse-project-waf) ─ ACM(HTTPS)
                        │
                        ↓ HTTP
                     EC2(project-app 서브넷, t4g.small)
                     Spring Boot, prod 프로필
                        ├─ 연동 예정 ─ RDS MySQL(project-storage 서브넷)
                        ├─ 연동 예정 ─ techcourse-project-2026 버킷(사진)  ← EC2 ec2-project role
                        ├─ /etc/jachwi-sunbae/app.env (운영 비밀, 0600)
                        └─ 연동 예정 ─ Google OAuth
```

배포 경로(코드 → 서비스)는 애플리케이션 트래픽과 분리된다. 상세 명령과 훅은 [백엔드 배포](../../backend/docs/operations/deployment.md)와 [프론트엔드 배포](../../frontend/docs/deployment.md)가 정본이다.

```text
PR → GitHub Actions 필수 검사 → 보호 브랜치 병합
  ├─ develop → dev 파이프라인
  └─ main    → prod 파이프라인
       ├─ 백엔드 Commands ─ jar 빌드(-x test) ─ CodeDeploy
       │    └─ Actuator health와 소스 SHA 확인
       └─ 프론트 Commands ─ build ─ S3 sync ─ CloudFront 무효화 완료
            └─ 실제 index.html의 번들 파일명 확인
```

## 4. 구성 요소별 설계

### 4.1 네트워크

- **VPC**: `TECHCOURSE-PROJECT`(`vpc-004e154d9f1f3f5cd`).
- **EC2 서브넷**: LB 뒤에 두므로 `project-app-a`, `project-app-b`.
- **ALB 서브넷**: `project-lb-a`, `project-lb-b`.
- **RDS 서브넷**: 서브넷 그룹 `project-rds-subnet-group`(`project-storage-a/b`).

보안 그룹은 이미 만들어진 것을 지정해 쓴다. 4종 모두 위 VPC에 있는 것을 확인했다(2026-08-14).

| 이름 | ID | 용도 |
| --- | --- | --- |
| `project-lb` | `sg-00be6776ff3c3aea2` | ALB |
| `project-app` | `sg-034df39fb4edbf0e6` | EC2 |
| `project-db` | `sg-0ce905a3e798a6a78` | RDS |
| `project-public` | `sg-017bc5d8159ac557e` | 이번 구성에서는 쓰지 않는다 |

보안 그룹 원칙: ALB는 외부에서 443만 받고, EC2 애플리케이션 포트는 `project-lb`에서 오는 트래픽만 허용한다. RDS 3306은 `project-app`에서 들어올 수 있어야 한다.

**`project-app`의 인바운드는 80·443(← `project-lb`)과 22(← `project-public`)뿐이다. 8080은 열려 있지 않다.** 이 그룹도 공용이라 규칙을 추가하면 다른 팀 인스턴스에도 열리므로 바꾸지 않는다. 대신 애플리케이션이 80을 직접 듣는다([4.2 컴퓨트](#42-컴퓨트-ec2)).

**`project-db`는 여러 팀이 공유하는 공용 보안 그룹이다.** 규칙이 9개 있고 3306이 `project-app` 외에 `project-public`에서도 열려 있다. 필요한 규칙(3306 ← `project-app`)은 이미 있으므로 추가할 것은 없다. 불필요한 규칙을 지우면 같은 그룹을 쓰는 다른 팀 DB가 끊기므로 **팀 임의로 수정하지 않는다.**

따라서 "3306을 `project-app`에서만 허용한다"는 원칙은 네트워크 계층에서 완전히 강제되지 않는다. 다음으로 방어한다.

- RDS 퍼블릭 액세스를 끈다. VPC 밖에서는 도달할 수 없다.
- 강한 마스터 암호를 쓰고 EC2의 `0600` 설정 파일에만 둔다.
- 애플리케이션은 마스터 계정이 아니라 필요한 권한만 가진 전용 DB 사용자로 접속한다.

**인터넷 egress는 확인했다(2026-08-13).** `project-app-a`(`subnet-0e693cde6a836c0b8`, `10.0.20.0/24`, `ap-northeast-2a`)에 라우팅 테이블 `rtb-project-private-a`(`rtb-03226c586ffce1d86`)가 명시적으로 연결되어 있고, `0.0.0.0/0`이 NAT 게이트웨이 `nat-0198ce7cbc3e952...`로 향한다. 상태는 활성이며 블랙홀이 아니다. 따라서 EC2는 사설 서브넷 `project-app`에 둔다. `project-public` + 퍼블릭 IP 대안은 채택하지 않는다.

**같은 VPC의 기본 라우팅 테이블 `rtb-project-default`에는 `0.0.0.0/0` 경로가 없다.** `10.0.0.0/16 → local`뿐이다. 라우팅 테이블이 명시적으로 연결되지 않은 서브넷에 EC2를 올리면 이 기본 테이블을 따르게 되어 인터넷으로 나가지 못한다. 이때 애플리케이션은 기동하지만 Google 토큰 엔드포인트에 나가지 못해 **로그인만 실패한다.** 원인을 찾기 어려운 종류의 실패이므로, EC2를 만들 때 선택한 서브넷의 라우팅 테이블 연결을 반드시 확인한다.

NAT 게이트웨이를 새로 만드는 선택지는 월 약 $32로 예산을 초과하므로 두지 않는다. 기존 NAT를 쓴다.

### 4.2 컴퓨트 (EC2)

2026-08-14에 생성했다.

| 항목 | 값 |
| --- | --- |
| 이름 | `jachwi-sunbae-prod` |
| AMI | Amazon Linux 2023 **arm64** |
| 타입 | `t4g.small` (2 vCPU / 2GiB) |
| 서브넷 | `project-app-a` (`subnet-0e693cde6a836c0b8`), 퍼블릭 IP 없음 |
| 보안 그룹 | `project-app` (`sg-034df39fb4edbf0e6`) |
| IAM 프로파일 | `ec2-project` |
| 루트 볼륨 | 20GiB |
| 키 페어 | 없음 |

- 타입은 `t4g.small`(ARM, 2GB RAM)로 시작한다. `t4g.micro`(1GB)는 JVM에 빠듯해 최후의 축소 카드로만 둔다.
- **AMI는 arm64여야 한다.** `t4g`는 ARM이므로 x86 AMI를 고르면 기동하지 않거나 CodeDeploy 에이전트가 붙지 않는다. 아키텍처는 인스턴스를 다시 만들지 않으면 바꿀 수 없다.
- 처음에는 Ubuntu로 만들었으나 빠른 시작 목록에 26.04만 있었다. CodeDeploy 에이전트는 AWS가 지원 목록에 올린 배포판에서만 검증되는데 26.04는 갓 나온 버전이라 확인되지 않았고, 설치가 실패하면 배포 파이프라인 전체가 막힌다. 검증된 조합을 택해 Amazon Linux 2023으로 다시 만들었다.
- 인스턴스에 IAM role `ec2-project`를 연결한다. 이 role로 S3(사진)·CloudWatch(로그)·CodeDeploy 산출물 접근을 액세스 키 없이 수행한다.
- **셸 접속은 세션 관리자로 한다.** `project-app`은 퍼블릭 IP가 없는 사설 서브넷이라 SSH로 직접 닿을 수 없고 키 페어도 두지 않았다. `ec2-project` role에 SSM 권한이 있어 세션 관리자로 접속되는 것을 확인했다. 접속 수단이 없으면 운영 비밀을 로컬 설정 파일에 둘 수 없으므로([4.8 비밀·환경변수](#48-비밀환경변수)) 이는 선택이 아니라 전제다. 서브넷과 키 페어는 둘 다 재생성 없이 바꿀 수 없으니 인스턴스를 다시 만들 때도 먼저 확인한다.
- CodeDeploy 에이전트와 애플리케이션 실행 런타임(JDK 21)을 설치한다. CodeDeploy 에이전트는 Ruby로 동작하므로 `ruby`가 함께 필요하다. 접속 수단이 확정되기 전이라도 준비되도록 사용자 데이터로 설치한다.

```bash
#!/bin/bash
dnf install -y java-21-amazon-corretto ruby wget
```

- SSM 에이전트와 AWS CLI v2는 Amazon Linux 2023에 기본 설치되어 있어 따로 넣지 않는다.
- 태그 3종을 설정한다.
- **애플리케이션은 8080이 아니라 80을 듣는다.** 보안 그룹이 8080을 허용하지 않기 때문이다. 비루트 계정이 1024 미만 포트에 바인딩하도록 systemd 유닛에서 `AmbientCapabilities=CAP_NET_BIND_SERVICE`를 준다.

생성 후 다음을 확인했다.

| 확인 | 결과 |
| --- | --- |
| 인스턴스 ID | `i-0f602d10ed2ace6c7` |
| 런타임 | Corretto 21.0.12, Ruby 3.2.8 (aarch64) |
| 역할 | `assumed-role/ec2-project/i-0f602d10ed2ace6c7` |
| 인터넷 egress | `https://oauth2.googleapis.com/` → `404`. 응답이 왔으므로 NAT 경로 정상 |
| S3 | `techcourse-project-2026` 목록 조회 가능 |

### 4.3 데이터베이스 (RDS)

2026-08-14에 생성했다.

| 항목 | 값 |
| --- | --- |
| 식별자 | `jachwi-sunbae-db` |
| 엔진 | MySQL Community 8.4.9 |
| 인스턴스 | `db.t4g.micro`, gp3 20GB, 단일 AZ(`ap-northeast-2c`) |
| 엔드포인트 | `jachwi-sunbae-db.cqsc6pyqhwww.ap-northeast-2.rds.amazonaws.com:3306` |
| 마스터 사용자 | `jachwi_admin` (암호는 EC2의 `0600` 설정 파일에만 둔다) |
| 자동 백업 | 7일 |
| 파라미터 그룹 | `default.mysql8.4` |

- 자동 백업과 수동 스냅샷을 사용한다. 이는 [롤백](../../backend/docs/operations/rollback.md)이 요구하는 "검증된 백업을 격리된 대상에 복구"를 실제로 가능하게 하는 근거다.
- 퍼블릭 액세스를 껐고 `project-storage` 서브넷에 있다.
- 자격 증명은 Secrets Manager가 아니라 자체 관리로 둔다. 비밀은 한 곳에 모으고([4.8 비밀·환경변수](#48-비밀환경변수)), Secrets Manager는 시크릿당 월 요금이 붙는다.
- **커스텀 파라미터 그룹을 만들지 않는다.** 기본 그룹의 `character_set_*`은 엔진 기본값(`-`)으로 보이지만, 마이그레이션이 테이블마다 `DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci`를 명시하므로 서버 기본값과 무관하게 `utf8mb4`로 만들어진다. 서버 설정에 의존하는 부분이 없다.
- 시간대는 UTC로 둔다([시스템 개요](../../backend/docs/architecture/system-overview.md)의 UTC 기준과 일치).

### 4.4 사진 저장소 (S3)

- 운영 버킷 `techcourse-project-2026`의 **팀 폴더** 아래에 사진 객체를 둔다. 접두사를 틀려도 버킷 이름만 맞으면 오류 없이 통과해 다른 팀 폴더에 쌓이므로, 설정한 접두사를 실제 업로드로 확인한다. 빌드 산출물은 별도 버킷(`techcourse-project-2026-artifacts`)을 사용한다.
- 로컬 MinIO와 운영 S3는 같은 애플리케이션 경계(`PhotoStorage`)를 쓴다([ADR-0006](../../backend/docs/adr/0006-use-private-s3-compatible-photo-storage.md)). 운영 전환에서 바뀌는 것은 **자격증명 주입 방식**이다. 로컬은 정적 키(MinIO 예시 값)를 쓰지만 운영은 정적 키를 두지 않고 EC2 `ec2-project` role로 접근한다.
- **이 버킷은 정책상 공개 읽기다.** `PublicReadGetObject` 문장이 `Principal: *`로 `s3:GetObject`를 허용하고 퍼블릭 액세스 차단도 꺼져 있다. 여러 팀이 공유하므로 팀 임의로 바꾸지 않는다.

  따라서 [ADR-0006](../../backend/docs/adr/0006-use-private-s3-compatible-photo-storage.md)이 전제한 비공개 저장소는 이 환경에서 성립하지 않는다. 사진 본문은 지금처럼 인증 백엔드가 소유권을 확인한 뒤 스트리밍하고 저장소 URL을 노출하지 않지만, 이는 애플리케이션 계층의 통제이고 저장소 자체는 URL을 아는 사람에게 열려 있다. 객체 키가 `members/{memberId}/properties/{propertyId}/{UUID}` 형태라 추측은 사실상 불가능하고 `s3:ListBucket`도 허용되어 있지 않다.

  서비스 동작에는 영향이 없어 현재 구성을 그대로 둔다. 팀 단위 비공개 정책을 쓸 수 있게 되면 다시 판단한다.

### 4.5 프론트엔드 (S3 + CloudFront)

- 실제 구성은 React 19 + TypeScript + Webpack 5, Node 22.23.1(`.nvmrc`), 라우팅은 react-router v7이다. 빌드는 `npm run build`(`webpack --mode production`)로 `dist/`를 만든다.
- 정적 SPA이므로 EC2가 아니라 S3에 올리고 CloudFront로 서빙한다. 프론트엔드 자율 요구사항의 `Cache Busting`·`CDN Cache Invalidation`·`contenthash`가 이 구조를 전제로 한다.
- CloudFront OAC는 인프라 안내가 지정한 `techcourse-project-2026.s3.ap-northeast-2.amazonaws.com`을 origin으로 사용한다.
- 캐시는 Policy를 새로 만들지 않는다. 새 콘솔에는 레거시 캐시 설정 항목이 없어 관리형 정책 `CachingOptimized`를 쓴다. 관리형이므로 새 정책을 만들지 않는다는 안내의 의도에 맞는다. 기본 TTL이 24시간이라 이름이 고정인 `index.html`은 배포마다 무효화한다.
- **SPA 폴백**: react-router 클라이언트 라우팅이므로 CloudFront에서 403·404 응답을 `/index.html`(200)로 매핑해 새로고침·딥링크가 깨지지 않게 한다. Google 콜백 경로 `/oauth/google/callback`도 프론트 라우트다.
- **환경변수는 빌드 타임에 주입된다.** `webpack.config.js`의 `DefinePlugin`이 `API_BASE_URL`·`GOOGLE_CLIENT_ID`·`GOOGLE_REDIRECT_URI`를 번들에 박아넣는다. 런타임 설정이 아니므로 운영 배포는 CodePipeline `Commands` 액션이 운영 값(`API_BASE_URL=https://api.<도메인>`, `GOOGLE_REDIRECT_URI=https://www.<도메인>/oauth/google/callback`)으로 **다시 빌드**해야 한다. 이 값들은 액션의 실행 환경변수로 전달한다. 어차피 번들에 박혀 공개되는 값이므로 비밀이 아니다.
- **캐시 무효화는 `contenthash`로 한다.** 운영 빌드의 파일명에 해시를 붙여 내용이 바뀌면 파일명이 바뀌게 한다. 배포마다 전체 무효화(`/*`)를 걸 필요가 없고, 이름이 고정인 `index.html`만 무효화하면 나머지는 자동으로 새 파일을 가리킨다. 개발 빌드에는 붙이지 않는다.
- **CloudFront origin path를 `/jachwi-sunbae/web`으로 지정한다.** 같은 버킷의 `jachwi-sunbae/` 아래에 비공개 사진 객체가 있다. origin path를 비워 두면 CloudFront가 버킷 전체를 공개해 사진이 인증 없이 노출된다.
- 절차는 [프론트엔드 배포](../../frontend/docs/deployment.md)에 있다.

### 4.6 진입 계층 (ALB + WAF + ACM)

- ALB를 `project-lb` 서브넷에 두고 443 HTTPS 리스너에 ACM 인증서를 붙인다. 80은 443으로 리다이렉트한다.
- 대상 그룹은 HTTP **80**으로 만든다. 헬스체크 경로는 `/actuator/health`다. **대상 그룹은 만든 뒤 포트를 바꿀 수 없다.**
- **WAF 연결은 필수다.** 공용 WAF `techcourse-project-waf`를 ALB에 연결한다. 연결하지 않으면 요청을 받지 못한다. 이 WAF 요금은 팀 예산에서 제외된다.
- **ACM 인증서는 리전에 주의한다.** ALB용 인증서는 서울(`ap-northeast-2`), CloudFront용 인증서는 **버지니아 북부(`us-east-1`)** 다. 리전을 잘못 고르면 나중에 붙지 않는다. 두 인증서 모두 DNS 검증을 가비아 DNS에 CNAME으로 추가했고 2026-08-13에 발급됐다.

| 용도 | 리전 | 도메인 | ARN |
| --- | --- | --- | --- |
| ALB | `ap-northeast-2` | `api.jachwi-sunbae.kr` | `arn:aws:acm:ap-northeast-2:843255971531:certificate/8a41c55b-5ed8-49de-8113-1fb632cb2391` |
| CloudFront | `us-east-1` | `www.jachwi-sunbae.kr` | `arn:aws:acm:us-east-1:843255971531:certificate/b7e879e2-578a-4f4d-ab70-59408bd95451` |

- 만료는 2027-02-27이다. **DNS 검증 CNAME 2개를 지우지 않는다.** ACM은 이 레코드로 자동 갱신하므로, 지우면 갱신에 실패해 HTTPS가 끊긴다.

### 4.7 도메인·DNS (가비아)

- 가비아에서 `jachwi-sunbae.kr`을 구매했고(2026-08-13 ~ 2027-08-13) 가비아 DNS에서 레코드를 관리한다.
- `api.jachwi-sunbae.kr` → ALB DNS 이름, `www.jachwi-sunbae.kr` → CloudFront 배포 도메인으로 향하는 CNAME을 둔다. ALB·CloudFront를 만든 뒤 추가한다.
- ACM DNS 검증용 CNAME과 서비스 레코드를 함께 관리한다.
- **apex(`jachwi-sunbae.kr`)는 서비스하지 않는다.** 가비아는 apex에 CNAME을 넣을 수 없으므로 서브도메인만 쓴다. 도메인을 그대로 입력한 사용자는 아무 곳에도 닿지 않는다.

  가비아 웹 포워딩을 검토했으나 **채택하지 않았다.** 이 기능은 `@`뿐 아니라 `www`에도 가비아 포워딩 서버를 가리키는 A 레코드를 만든다. 한 호스트에 CNAME과 A는 공존할 수 없으므로 `www`의 CloudFront CNAME이 밀려나 사이트 전체가 뜨지 않게 된다.

  AWS로 처리하려면 리다이렉트 전용 S3 버킷과 CloudFront 배포, apex를 포함한 `us-east-1` 인증서가 더 필요하다. 비용은 거의 0이지만 **만든 뒤 지울 수 없는 리소스가 둘 늘어난다.** 팀에 삭제 권한이 대부분 없고 태그 없는 리소스는 종료 대상이다. 사용자는 안내받은 `https://www.jachwi-sunbae.kr`로 들어오므로 얻는 것에 비해 대가가 크다고 판단했다. apex 유입이 실제로 문제가 되면 그때 만든다.
- ACM 검증 CNAME 등록 시 가비아는 입력한 호스트에 도메인을 자동으로 붙인다. ACM이 준 이름에서 도메인 접미사를 뺀 부분만 넣고, 등록 뒤 공개 조회로 실제 값을 확인한다.
- 운영 값은 다음과 같다. wildcard는 쓰지 않는다. Google OAuth 콘솔의 허용 redirect URI에도 같은 값을 등록한다.

| 환경변수 | 값 |
| --- | --- |
| `CORS_ALLOWED_ORIGINS` | `https://www.jachwi-sunbae.kr` |
| `GOOGLE_OAUTH_ALLOWED_REDIRECT_URIS` | `https://www.jachwi-sunbae.kr/oauth/google/callback` |

### 4.8 비밀·환경변수

- 운영 프로필 `prod`를 신설한다. 로컬 기본값([환경변수](../../backend/docs/guides/environment-variables.md))과 분리한다.
- **SSM Parameter Store는 쓰지 않는다.** IAM 사용자로는 권한이 없어 접근할 수 없고, EC2 인스턴스 role로는 동작하지만 **계정 전체가 한 범위다.** `describe-parameters`를 실행하면 다른 팀이 만든 파라미터가 그대로 보인다. 우리 비밀을 두면 다른 팀 인스턴스에서도 읽을 수 있으므로 운영 비밀을 두기에 적절하지 않다. 비밀이 아닌 값이라면 나중에 옮길 수 있다.

- 대신 **EC2 로컬 설정 파일**에 운영 값을 둔다. `/etc/jachwi-sunbae/app.env`를 소유자 `root`, 권한 `0600`으로 한 번 만든다. CodeDeploy 배포 훅이 이 파일을 읽어 애플리케이션에 환경변수로 전달한다.
- 이 파일은 **배포 산출물에 포함하지 않는다.** CodeDeploy가 덮어쓰는 경로 밖에 두어 배포마다 값이 사라지지 않게 한다.
- 실제 비밀은 저장소·문서·`.env.example`에 커밋하지 않는다는 원칙을 그대로 유지한다.
- **사용자 데이터에 비밀을 넣지 않는다.** 사용자 데이터는 인스턴스 메타데이터로 노출되어 인스턴스 안의 무엇이든 읽을 수 있다.

현재 초기화된 백엔드 애플리케이션은 운영 비밀값을 사용하지 않는다. systemd 계약을 유지하기 위해 파일은 빈 상태로 둘 수 있으며, 실제 연동을 추가할 때 필요한 값과 비밀 여부를 다시 정한다.

이 방식의 대가를 분명히 해둔다. 값을 바꾸려면 사람이 서버에 접속해야 하고, 비밀이 서버 디스크에 평문으로 남으며, 인스턴스를 다시 만들면 파일을 다시 만들어야 한다. 이력도 남지 않는다. 대신 파일 권한이 곧 경계이므로 다른 팀이 읽을 수 없다. 팀 전용 비밀 저장소를 쓸 수 있게 되면 다시 판단한다.

**셸 접속이 전제 조건이다.** 이 파일을 두려면 인스턴스에 들어가야 한다. 세션 관리자로 접속되는 것을 확인했다([4.2 컴퓨트](#42-컴퓨트-ec2)).

## 5. 배포 자동화 파이프라인

액세스 키를 만들 수 없으므로 **AWS 네이티브 파이프라인**으로 구성한다. 모든 단계가 제공된 service role로 동작한다.

1. **병합 전 검증**: `main`과 `develop`의 보호 규칙이 GitHub Actions의 백엔드·프론트엔드·문서 검사를 필수로 요구한다.
2. **소스**: CodePipeline 소스 공급자는 GitHub(버전 1). `main`은 prod, `develop`은 dev 파이프라인을 트리거한다.
3. **백엔드 빌드**: CodePipeline `Commands`가 테스트를 제외하고 jar를 만든다. GitHub Actions에서 이미 검사한 커밋만 병합되므로 테스트를 반복하지 않는다. 소스 SHA는 jar의 build-info와 배포 리비전 파일에 함께 기록한다.
4. **백엔드 배포·검증**: CodeDeploy가 EC2에 배포한다. `appspec.yml`과 훅 스크립트가 프로세스를 재시작하고 Actuator health와 실행 중인 소스 SHA를 확인한다.
5. **프론트엔드 배포·검증**: 별도 `Commands` 액션이 빌드 결과를 환경별 S3 경로에 동기화한다. CloudFront `index.html` 무효화가 끝난 뒤 실제 응답이 이번 번들 파일명을 참조하는지 확인한다.

네 파이프라인은 제공된 CodePipeline service role로 동작한다. 백엔드와 프론트엔드를 별도 파이프라인으로 두어 한쪽 실패가 다른 쪽 배포를 막지 않게 한다.

같은 환경의 백엔드와 프론트엔드 파이프라인은 같은 브랜치 push를 소스로 사용하고 AWS 트리거에 디렉터리 필터가 없다. 따라서 백엔드만 바뀐 병합도 두 파이프라인을 모두 시작한다. GitHub Actions의 내부 변경 감지는 병합 전 검사량만 줄이며 CodePipeline 실행 여부를 제어하지 않는다.

PR 검증은 GitHub Actions가 맡고 배포용 산출물 생성과 실제 서비스 검증은 CodePipeline이 맡는다. 두 경계를 보호 규칙과 배포 리비전 비교로 연결한다.

## 6. 데이터베이스 변경이 포함된 배포

현재 백엔드는 데이터베이스에 연결하지 않으며 마이그레이션 도구를 사용하지 않는다. 데이터베이스 연동과 변경 절차는 실제 구현을 시작할 때 다시 결정한다.

## 7. 롤백

- 애플리케이션 자동 롤백은 CodePipeline 스테이지가 직전 정상 실행을 다시 배포한다. 롤백 실행의 초록불 뒤에도 실제 `/actuator/info`가 직전 정상 SHA를 반환해야 완료다. 판단 기준과 검증 절차는 [롤백](../../backend/docs/operations/rollback.md)을 따른다.

## 8. 비용 추정

서울 리전 기준 월 환산(대략)이다. 실제 8월은 배포 시점 이후 일수만 과금되므로 아래보다 낮다.

| 항목 | 사양 | 월 환산(대략) |
| --- | --- | --- |
| EC2 (prod) | `t4g.small` 1대 | ~$15 |
| EC2 (dev) | `t4g.micro` 1대 | ~$8 |
| RDS | `db.t4g.micro` + gp3 20GB | ~$15 |
| ALB | 기동 시간요금 + 소량 LCU | ~$17 |
| WAF | 공용 `techcourse-project-waf` | $0 (팀 예산 제외) |
| ACM | 퍼블릭 인증서 | $0 |
| S3·CloudFront·전송량 | 소량 | ~$2 |
| **합계** | | **~$57** |

- ACM은 무료다. WAF 요금은 팀 예산에서 제외한다는 답변을 받았으므로(2026-08-14) 설계를 바꾸지 않고 ALB + WAF를 그대로 간다.
- **dev 환경은 EC2 한 대만 추가한다.** RDS·ALB·WAF·S3는 prod와 공유한다. 따로 만들면 RDS +$15, ALB +$17이 더 든다.
- 8월은 남은 일수만 과금되므로 한도 초과 위험이 낮다. 예산 판단의 기준 달은 처음으로 한 달을 꽉 채우는 9월이다. **$60 한도에 여유가 약 $3뿐이므로** 전송량과 ALB LCU를 주시한다.
- dev EC2를 `t4g.small`로 올리면 ~$64로 한도를 넘는다. 업무 시간만 켜는 방식이 더 싸지만 자동화에 IAM Role이 필요하고 팀은 role을 만들 수 없다. 사람이 잊으면 그 달 예산이 넘으므로 채택하지 않았다.
- 여유가 줄면 축소 순서는 ALB 제거(EC2에 직접 HTTPS) → EC2 `t4g.micro` 축소다. 다만 WAF 연결이 필수라 ALB 제거는 인프라 안내와 충돌하므로 먼저 `#8기-기술-검토`에 문의한다.

## 9. 미결 사항

| 항목 | 상태 | 필요한 확인 |
| --- | --- | --- |
| RDS를 두 환경이 공유 | 감수함 | dev 부하가 prod 성능에 영향을 줄 수 있다. `db.t4g.micro`는 1GB에 vCPU 2개다. 문제가 되면 dev용 RDS를 분리한다 |
| WAF 연결 확인 | 확인 불가 | IAM 사용자에게 `wafv2:ListResourcesForWebACL`·`wafv2:GetWebACLForResource` 권한이 없어 콘솔에서 연결 여부를 볼 수 없다. 연결 작업은 오류 없이 끝났고 요청도 통과한다 |
| 운영 모니터링 | 미구성 | 배포 성공 여부는 확인하지만 운영 중 지표는 수집하지 않는다 |

## 10. 검토한 대안

| 결정 | 채택 | 검토한 대안 | 채택하지 않은 이유 |
| --- | --- | --- | --- |
| 배포 자동화 | CodePipeline `Commands`+CodeDeploy | GitHub Actions self-hosted runner를 EC2에 설치 | 셋업은 더 단순하나, 팀이 AWS 네이티브 CI/CD 학습을 자율 요구사항으로 가져갈 수 있어 학습 가치가 큰 쪽을 택함. 러너 방식은 축소 대안으로 유지 |
| 데이터베이스 | RDS MySQL | EC2에 MySQL 직접 설치 | 비용은 낮으나 백업·복구·운영 부담이 크고, 롤백 절차의 백업 복구 전제와 맞지 않음 |
| 비밀 관리 | EC2 로컬 설정 파일 | SSM Parameter Store(SecureString), AWS Secrets Manager | Parameter Store는 인스턴스 role로 동작하지만 계정 전체가 한 범위라 다른 팀 파라미터까지 보인다. 팀 간 격리가 없어 운영 비밀을 두기에 맞지 않다. Secrets Manager는 시크릿당 월 요금이 붙어 예산에 부담이 된다 |
| EC2 운영체제 | Amazon Linux 2023 (arm64) | Ubuntu (arm64) | 팀에 익숙하고 참고 자료가 많아 우분투를 먼저 택했으나, 빠른 시작 목록에 26.04만 있었다. CodeDeploy 에이전트는 AWS가 지원 목록에 올린 배포판에서만 검증되고 26.04는 갓 나온 버전이라 확인되지 않았다. 파이프라인 전체를 막을 수 있는 위험이라 검증된 조합을 택했다. SSM 에이전트와 AWS CLI v2가 기본 설치되는 이점도 있다 |
| 프론트 서빙 | S3+CloudFront | EC2에 nginx로 함께 서빙 | 정적 SPA에 CDN 캐싱 이점이 크고 백엔드와 장애가 분리됨. 프론트엔드 캐시 요구사항과도 맞음 |
| 진입 계층 | ALB+WAF+ACM | EC2에 직접 도메인·HTTPS(certbot) | ACM·WAF는 EC2에 직접 붙지 않고, "요청 수신에 WAF 필요" 요건을 EC2 단독으로 충족할 수 없음. 비용 압박 시 CloudFront 앞단으로 대체 검토 |
