# 로컬 개발

- 문서 성격: 파생
- 대조 대상: `backend/compose.yaml`, `backend/build.gradle`, `frontend/package.json`, 실행 설정

## 1. 준비물

| 항목 | 기준 |
| --- | --- |
| JDK | Java 21 |
| Docker | MySQL·MinIO 컨테이너를 실행할 수 있는 버전 |
| Node.js | `frontend/.nvmrc`와 같은 버전 |
| Git | GitHub 저장소를 복제할 수 있는 버전 |
| HTTP 확인 도구 | `curl` 또는 브라우저 |

Gradle은 별도로 설치하지 않는다. 저장소의 Gradle Wrapper를 사용한다.

## 2. 환경변수 준비

`backend`에서 개인 환경변수 파일을 만든다.

```bash
cp .env.example .env
```

Google OAuth 값과 JWT 비밀키를 개인 개발 값으로 바꾼다. `.env`는 Git에 커밋하지 않는다. 전체 목록은 [환경변수](environment-variables.md)를 따른다.

## 3. 로컬 인프라 실행

`backend`에서 실행한다.

```bash
docker compose up -d
docker compose ps
```

MySQL과 MinIO가 healthy여야 한다. 빈 MySQL 볼륨은 현재 스키마와 기본 데이터로 자동 초기화된다. SQL을 바꾼 뒤 기존 볼륨을 다시 만드는 절차와 데이터 삭제 주의사항은 [데이터베이스 초기화](database-initialization.md)를 따른다.

## 4. 백엔드 실행

`backend`에서 실행한다.

```bash
set -a
source .env
set +a
./gradlew bootRun
```

로컬 CORS는 `CORS_ALLOWED_ORIGINS=http://localhost:3000`을 사용한다.

| 확인 항목 | 주소 |
| --- | --- |
| 서버 상태 | `http://localhost:8080/actuator/health` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |

```bash
curl --fail http://localhost:8080/actuator/health
curl --fail http://localhost:8080/v3/api-docs
```

두 요청이 성공해야 한다. API 계약은 별도 Markdown 문서로 관리하지 않고 구현과 함께 생성되는 Swagger/OpenAPI를 사용한다.

## 5. 프론트엔드 실행

별도 터미널의 `frontend`에서 실행한다.

```bash
npm ci
API_BASE_URL=http://localhost:8080 \
GOOGLE_CLIENT_ID=<로컬-Google-Client-ID> \
GOOGLE_REDIRECT_URI=http://localhost:3000/oauth/google/callback \
npm run dev
```

브라우저에서 `http://localhost:3000`을 연다. Google Console의 허용 redirect URI에도 같은 callback 주소를 등록한다.

## 6. 검사

```bash
./gradlew test --no-daemon
```

프론트엔드는 변경 범위에 맞춰 `npm run build`를 실행한다. 전체 타입·린트·포맷·테스트가 필요할 때는 `frontend/README.md`의 검사 명령을 따른다.

## 7. 확인 목록

- [ ] `./gradlew bootRun`으로 애플리케이션이 실행된다.
- [ ] Actuator health가 `UP`을 응답한다.
- [ ] Swagger UI와 OpenAPI JSON에 접근할 수 있다.
- [ ] Docker MySQL과 MinIO가 healthy다.
- [ ] `./gradlew test --no-daemon`이 성공한다.
- [ ] 프론트엔드가 백엔드 `/api` 요청을 보낼 수 있다.
