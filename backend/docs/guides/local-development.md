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

기본 예시는 외부 키가 필요 없는 닉네임 인증·`demo` 지도와 로컬 MinIO 값을 포함한다. `.env`는 Git에 커밋하지 않는다. 전체 목록은 [환경변수](environment-variables.md)를 따른다.

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

두 요청이 성공해야 한다. 실제 실행 계약은 구현에서 생성되는 Swagger/OpenAPI이며 [MVP2 API 계약](../api/mvp2-api-contract.md)은 endpoint 대응과 핵심 불변식만 요약한다.

## 5. 프론트엔드 실행

별도 터미널의 `frontend`에서 실행한다.

```bash
npm ci
cp .env.example .env.local
set -a
source .env.local
set +a
npm run dev
```

브라우저에서 `http://localhost:3000`을 열고 닉네임을 입력한다. 비밀번호를 비우면 같은 닉네임을 입력한 사람이 기록을 함께 사용할 수 있고, 처음 사용할 때 비밀번호를 입력하면 이후 같은 비밀번호가 필요하다. `이자취`는 비밀번호 없는 데모 데이터와 연결되며 백엔드가 최초 실행 시 멱등하게 만든다.

실제 Kakao를 확인할 때만 양쪽 `.env`의 지도 adapter 모드를 바꾸고 [지도 외부 연동](map-integration.md)을 따른다.

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
- [ ] 닉네임으로 시작한 뒤 매물·사진·메모·체크리스트·2~5개 매물 비교 PDF·지도 흐름이 동작한다.
