# 환경변수

- 문서 성격: 파생
- 대조 대상: `backend/.env.example`

## 관리 원칙

- 예시와 기본값은 `backend/.env.example`에 기록하고 Git에 커밋한다.
- 개인 값은 `backend/.env`에 기록하며 Git에 커밋하지 않는다.
- 실제 비밀번호, 토큰과 운영 비밀값은 문서, 코드, 예시 파일에 기록하지 않는다.
- 환경변수를 추가하거나 이름을 변경하면 애플리케이션 설정, Compose, `.env.example`과 이 문서를 같은 PR에서 수정한다.

## 현재 환경변수

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
| `PHOTO_STORAGE_ENDPOINT` | `http://localhost:9000` | S3 호환 객체 저장소 API endpoint |
| `PHOTO_STORAGE_REGION` | `us-east-1` | S3 서명에 사용하는 region |
| `PHOTO_STORAGE_BUCKET` | `jachwi-sunbae-photos` | 비공개 사진 객체 bucket |
| `PHOTO_STORAGE_ACCESS_KEY` | 로컬 전용 예시 값 | 객체 저장소 access key |
| `PHOTO_STORAGE_SECRET_KEY` | 로컬 전용 예시 값 | 객체 저장소 secret key |
| `PHOTO_STORAGE_PORT` | `9000` | 로컬 MinIO API 포트 |
| `PHOTO_STORAGE_CONSOLE_PORT` | `9001` | 로컬 MinIO 관리 화면 포트 |

## 사용 방법

`backend`에서 개인 파일을 생성한다.

```bash
cp .env.example .env
```

`JWT_SECRET_BASE64`, `GOOGLE_OAUTH_CLIENT_ID`, `GOOGLE_OAUTH_CLIENT_SECRET`의 placeholder를 실제 로컬 값으로 바꾼다. 사진 저장소의 로컬 예시 자격증명은 Compose 전용이며 운영에서 재사용하지 않는다. JWT 비밀키는 Base64 디코딩 후 32바이트 이상이어야 한다. 예를 들어 다음 명령으로 로컬 키를 만들 수 있다.

```bash
openssl rand -base64 32
```

Docker Compose는 같은 디렉터리의 `.env`를 자동으로 읽는다. Spring Boot 프로세스는 `.env`를 자동으로 읽지 않으므로 기본값이 아닌 값을 사용할 때는 실행 전에 환경변수를 전달한다.

```bash
set -a
source .env
set +a
./gradlew bootRun
```

운영 환경의 비밀 관리 방식은 배포 환경을 선택할 때 별도로 결정한다.

실제 JWT 비밀키, Google 인증정보와 운영 객체 저장소 자격증명은 `.env.example`, 애플리케이션 설정, 문서와 Git에 커밋하지 않는다. 목록형 환경변수에는 정확한 Origin과 redirect URI만 입력하며 wildcard를 사용하지 않는다.
