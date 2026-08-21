# 환경변수

- 문서 성격: 파생
- 대조 대상: `backend/.env.example`

## 관리 원칙

Spring Boot 애플리케이션은 CORS 허용 Origin과 인증·저장소 설정을 환경변수로 주입받는다. 아래 값 중 로컬 인프라용 값은 Docker Compose에서 사용하고, 애플리케이션 설정에 연결된 값은 실행 환경에 맞게 제공한다.

- 예시와 기본값은 `backend/.env.example`에 기록하고 Git에 커밋한다.
- 개인 값은 `backend/.env`에 기록하며 Git에 커밋하지 않는다.
- 실제 비밀번호, 토큰과 운영 비밀값은 문서, 코드, 예시 파일에 기록하지 않는다.
- 환경변수를 추가하거나 이름을 변경하면 애플리케이션 설정, Compose, `.env.example`과 이 문서를 같은 PR에서 수정한다.

## 로컬 인프라용 환경변수

| 환경변수 | 로컬 기본값 | 용도 |
| --- | --- | --- |
| `DB_HOST` | `localhost` | MySQL 호스트 |
| `DB_PORT` | `3306` | MySQL 포트 |
| `DB_NAME` | `jachwi_sunbae` | 데이터베이스 이름 |
| `DB_USERNAME` | `jachwi_sunbae` | 애플리케이션 계정 |
| `DB_PASSWORD` | `local_password` | 애플리케이션 계정 비밀번호 |
| `DB_ROOT_PASSWORD` | `local_root_password` | 로컬 MySQL root 비밀번호 |
| `JWT_SECRET_BASE64` | `replace-with-base64-encoded-32-byte-secret` | HS256 서명용 Base64 인코딩 비밀키 |
| `GOOGLE_OAUTH_CLIENT_ID` | `replace-with-google-oauth-client-id` | Google Web OAuth Client ID |
| `GOOGLE_OAUTH_CLIENT_SECRET` | `replace-with-google-oauth-client-secret` | Google Web OAuth Client Secret |
| `GOOGLE_OAUTH_ALLOWED_REDIRECT_URIS` | `http://localhost:3000/oauth/google/callback` | 쉼표로 구분한 허용 callback URI 목록 |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | 쉼표로 구분한 프론트엔드 Origin 허용 목록 |
| `PHOTO_STORAGE_ENDPOINT` | `http://localhost:9000` | S3 호환 객체 저장소 API endpoint. 정적 자격증명으로 접속하는 환경에서만 쓴다 |
| `PHOTO_STORAGE_REGION` | `us-east-1` | S3 서명에 사용하는 region |
| `PHOTO_STORAGE_BUCKET` | `jachwi-sunbae-photos` | 비공개 사진 객체 bucket |
| `PHOTO_STORAGE_KEY_PREFIX` | 비움 | 객체 key 앞에 붙일 경로. 버킷을 다른 팀과 공유할 때 사용하며 로컬은 전용 버킷이라 비운다 |
| `PHOTO_STORAGE_ACCESS_KEY` | 로컬 전용 예시 값 | 객체 저장소 access key. 정적 자격증명으로 접속하는 환경에서만 쓴다 |
| `PHOTO_STORAGE_SECRET_KEY` | 로컬 전용 예시 값 | 객체 저장소 secret key. 정적 자격증명으로 접속하는 환경에서만 쓴다 |
| `PHOTO_STORAGE_PORT` | `9000` | 로컬 MinIO API 포트 |
| `PHOTO_STORAGE_CONSOLE_PORT` | `9001` | 로컬 MinIO 관리 화면 포트 |

## 사용 방법

`backend`에서 개인 파일을 생성한다.

```bash
cp .env.example .env
```

Docker Compose는 같은 디렉터리의 `.env`를 자동으로 읽는다. Spring Boot를 직접 실행할 때는 셸 또는 실행 환경에 필요한 값을 주입한다.

## 운영 프로필

운영은 `prod` 프로필로 기동한다. 운영 환경변수는 EC2의 `/etc/jachwi-sunbae/app.env` 파일로 주입한다. `CORS_ALLOWED_ORIGINS`에는 해당 프론트엔드 Origin을 포함해야 한다. 새 환경변수를 도입할 때 구성은 [배포 아키텍처 설계](../../../docs/operations/deployment-architecture.md)와 [배포](../operations/deployment.md)에 함께 반영한다.

실제 비밀값은 `.env.example`, 애플리케이션 설정, 문서와 Git에 커밋하지 않는다.
