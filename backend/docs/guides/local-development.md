# 로컬 개발

## 1. 준비물

| 항목 | 기준 |
| --- | --- |
| JDK | Java 21 |
| Docker | Docker Engine과 Docker Compose를 사용할 수 있는 환경 |
| Git | GitHub 저장소를 복제할 수 있는 버전 |
| HTTP 확인 도구 | `curl` 또는 브라우저 |

Gradle과 MySQL은 별도로 설치하지 않는다. Gradle Wrapper와 Docker Compose가 팀 버전을 준비한다.

```bash
git clone https://github.com/woowacourse-teams/2026-jachwi-sunbae.git
cd 2026-jachwi-sunbae/backend
java -version
docker --version
docker compose version
```

## 2. 환경변수 준비

기본값을 그대로 사용하면 `.env` 없이도 실행할 수 있다. 개인 설정이 필요할 때만 생성한다.

```bash
cp .env.example .env
```

세부 항목과 Spring 프로세스 전달 방법은 [환경변수 가이드](environment-variables.md)를 참고한다.

## 3. MySQL 실행

`backend`에서 실행한다.

```bash
docker compose up -d mysql
docker compose ps
```

`mysql` 상태가 `healthy`면 준비가 완료된 것이다.

이 상태는 MySQL 컨테이너가 요청을 받을 준비가 됐다는 뜻이며, 백엔드의 연결 성공은 다음 단계의 Actuator health로 확인한다.

## 4. 백엔드 실행

```bash
./gradlew bootRun
```

| 확인 항목 | 주소 |
| --- | --- |
| 서버 상태 | `http://localhost:8080/actuator/health` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |

Actuator health가 `{"status":"UP"}`을 응답하면 애플리케이션과 MySQL 연결이 정상이다.

```bash
curl --fail http://localhost:8080/actuator/health
```

HTTP 요청이 성공하고 `{"status":"UP"}`이 출력되어야 한다. DB 연결에 실패하면 전체 health가 `DOWN`이 되고 요청도 실패한다.

## 5. 테스트와 빌드

Docker가 실행 중인 상태에서 새 터미널을 열고 `backend`로 이동한다. Compose MySQL은 사용하지 않으며 Testcontainers가 테스트용 MySQL을 자동으로 생성하고 정리한다.

```bash
cd 2026-jachwi-sunbae/backend
./gradlew test
./gradlew clean build --no-daemon
```

두 명령 모두 `BUILD SUCCESSFUL`로 끝나야 한다.

## 6. Flyway 상태

현재 프로젝트는 Flyway를 사용하지 않는다. Flyway 의존성과 `db/migration` 스크립트가 없으므로 확인할 Migration도 없다.

Flyway를 도입하면 애플리케이션 시작 로그, `flyway_schema_history` 테이블과 전체 테스트로 Migration 성공을 확인한다. 구체적인 도입 시점과 검증 방법은 첫 스키마를 만들기 전에 ADR로 결정한다.

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

- [ ] `docker compose ps`에서 MySQL이 `healthy`다.
- [ ] `./gradlew bootRun`으로 애플리케이션이 실행된다.
- [ ] Actuator health가 HTTP 성공과 `UP`을 응답해 MySQL 연결을 확인했다.
- [ ] Swagger UI에 접근할 수 있다.
- [ ] `./gradlew test`가 성공한다.
- [ ] `./gradlew clean build --no-daemon`이 성공한다.
- [ ] 현재 Flyway 미사용 상태를 확인했다.
