# 로컬 개발

## 1. 준비물

| 항목 | 기준 |
| --- | --- |
| JDK | Java 21 |
| Docker | Docker Engine과 Docker Compose를 사용할 수 있는 환경 |
| Git | GitHub 저장소를 복제할 수 있는 버전 |

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

## 5. 종료와 초기화

기본 종료는 로컬 데이터 볼륨을 유지한다.

```bash
docker compose down
```

로컬 데이터까지 초기화해야 할 때만 다음 명령을 사용한다.

> 주의: 다음 명령은 로컬 MySQL 데이터를 모두 삭제한다.

```bash
docker compose down --volumes
```

## 6. 재현 완료 체크리스트

- [ ] `docker compose ps`에서 MySQL이 `healthy`다.
- [ ] `./gradlew bootRun`으로 애플리케이션이 실행된다.
- [ ] Actuator health가 `UP`을 응답한다.
- [ ] Swagger UI에 접근할 수 있다.
- [ ] [테스트와 빌드](test-and-build.md)의 전체 빌드가 성공한다.
