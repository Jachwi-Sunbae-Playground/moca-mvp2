# 로컬 개발

## 1. 준비물

| 항목 | 기준 |
| --- | --- |
| JDK | Java 21 |
| Docker | Docker Engine과 Docker Compose로 MySQL·MinIO를 실행할 수 있는 환경 |
| Git | GitHub 저장소를 복제할 수 있는 버전 |
| HTTP 확인 도구 | `curl` 또는 브라우저 |

Gradle과 MySQL은 별도로 설치하지 않는다. Gradle Wrapper와 Docker Compose가 팀 버전을 준비한다.

```bash
git clone https://github.com/Jachwi-Sunbae-Playground/moca.git
cd moca/backend
java -version
docker --version
docker compose version
```

## 2. 환경변수 준비

DB만 기본값이 있다. 인증 비밀값과 Google OAuth 설정은 시작 시 검증하므로 `.env`를 만들고 placeholder를 실제 로컬 값으로 바꾼다.

```bash
cp .env.example .env
```

JWT 비밀키 생성과 세부 항목은 [환경변수 가이드](environment-variables.md)를 참고한다. Google Web OAuth Client에는 로컬 callback URI를 등록해야 한다.

## 3. MySQL과 객체 저장소 실행

`backend`에서 실행한다.

```bash
docker compose up -d mysql minio minio-init
docker compose ps
```

`mysql`과 `minio` 상태가 `healthy`이고 `minio-init`이 종료 코드 0이면 준비가 완료된 것이다. `minio-init`은 비공개 사진 bucket을 멱등 생성하고 종료한다.

이 상태는 MySQL 컨테이너가 요청을 받을 준비가 됐다는 뜻이며, 백엔드의 연결 성공은 다음 단계의 Actuator health로 확인한다.

## 4. 백엔드 실행

```bash
set -a
source .env
set +a
./gradlew bootRun
```

| 확인 항목 | 주소 |
| --- | --- |
| 서버 상태 | `http://localhost:8080/actuator/health` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |

Actuator health가 `{"status":"UP"}`을 응답하면 애플리케이션과 MySQL 연결이 정상이다. 객체 저장소 연결은 실제 사진 업로드·본문 조회·삭제 요청으로 확인하며 저장소 endpoint와 내부 키는 API에 노출되지 않는다.

`/v3/api-docs`는 최초 생성과 캐시된 반복 요청을 각각 확인한다. 두 요청 모두 `200`이고 비즈니스 연산 27개, 공개 1개·Bearer 보호 26개이며 어떤 operation도 인증 내부 `memberId`를 입력 parameter로 노출하지 않아야 한다. 최초 요청 로그에 `Json Processing Exception occurred`가 있으면 정상으로 보지 않는다.

애플리케이션과 MySQL 컨테이너의 기본 시간대는 UTC다. 사용자 화면에서만 한국 시간으로 변환한다.

```bash
curl --fail http://localhost:8080/actuator/health
```

HTTP 요청이 성공하고 `{"status":"UP"}`이 출력되어야 한다. DB 연결에 실패하면 전체 health가 `DOWN`이 되고 요청도 실패한다.

## 5. 테스트와 빌드

Docker가 실행 중인 상태에서 새 터미널을 열고 `backend`로 이동한다. Compose MySQL은 사용하지 않으며 Testcontainers가 테스트용 MySQL을 자동으로 생성하고 정리한다.

```bash
cd moca/backend
./gradlew test
./gradlew testAll --no-daemon
```

`test`는 인수 테스트를 제외한 빠른 검증이고 `testAll`은 실제 HTTP 인수 테스트까지 포함한 전체 검증이다. 두 명령 모두 `BUILD SUCCESSFUL`로 끝나야 한다. `clean build` 패키징은 GitHub Backend CI에서 검증한다.

## 6. 스키마 초기화와 Flyway 상태

Flyway가 애플리케이션 시작 전에 `src/main/resources/db/migration`의 V1~V4를 순서대로 적용한다. 빈 DB에는 제품 테이블 13개, 제공 체크 항목 72개와 프리셋 6개가 생성된다. v1.1에서는 ONE_ROOM 프리셋 3개만 활성 상태이고 GOSHIWON 프리셋·매핑 데이터는 삭제하지 않은 채 비활성화한다.

```bash
docker compose exec mysql \
  mysql -u"$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" \
  -e "SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank; SHOW TABLES;"
```

정상 상태는 성공한 V1~V4 네 행이다. 같은 DB에서 애플리케이션을 재시작해도 이력 행이 추가되지 않는다. 기존 pre-Flyway v1.0 DB는 기본 기동으로 자동 승인하지 않는다. [데이터베이스 마이그레이션 가이드](database-migrations.md)에 따라 백업·복구 리허설과 명시적 baseline을 먼저 수행한다.

## 7. 종료와 초기화

기본 종료는 로컬 데이터 볼륨을 유지한다.

```bash
docker compose down
```

로컬 데이터까지 초기화해야 할 때만 다음 명령을 사용한다.

> 주의: 다음 명령은 로컬 MySQL 데이터를 모두 삭제한다.

```bash
docker compose down --volumes
```

## 8. 재현 완료 체크리스트

- [ ] `docker compose ps`에서 MySQL과 MinIO가 `healthy`고 bucket 초기화가 성공했다.
- [ ] `./gradlew bootRun`으로 애플리케이션이 실행된다.
- [ ] Actuator health가 HTTP 성공과 `UP`을 응답해 MySQL 연결을 확인했다.
- [ ] Swagger UI에 접근할 수 있다.
- [ ] 최초·반복 OpenAPI 요청이 200이고 27개 연산·공개 1개·보호 26개·내부 `memberId` parameter 0개이며 schema 변환 경고가 없다.
- [ ] `./gradlew test`가 성공한다.
- [ ] `./gradlew testAll --no-daemon`이 성공한다.
- [ ] `flyway_schema_history`에 성공한 V1~V4가 순서대로 기록됐다.
- [ ] 13개 제품 테이블과 72개 기준 항목·6개 프리셋이 초기화됐다.
- [ ] ONE_ROOM 프리셋 3개만 활성이고 GOSHIWON 데이터는 보존된 비활성 상태다.
- [ ] 핵심 복합 소유권 FK, 단계 제약, 삭제 정책과 정렬 인덱스가 생성됐다.
- [ ] Swagger UI에서 API-001~002, API-101~106, API-201~204, API-301~307, API-401~402, API-501~506을 확인했다.
