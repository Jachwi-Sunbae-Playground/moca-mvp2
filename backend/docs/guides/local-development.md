# 로컬 개발

## 1. 준비물

| 항목 | 기준 |
| --- | --- |
| JDK | Java 21 |
| Git | GitHub 저장소를 복제할 수 있는 버전 |
| HTTP 확인 도구 | `curl` 또는 브라우저 |

Gradle은 별도로 설치하지 않는다. 저장소의 Gradle Wrapper를 사용한다.

## 2. 백엔드 실행

`backend`에서 실행한다.

```bash
./gradlew bootRun
```

로컬 실행에는 Java 21과 애플리케이션 환경변수가 필요하다. 개인 값은 `backend/.env`에 두고 저장소에 커밋하지 않는다. CORS를 확인하려면 `CORS_ALLOWED_ORIGINS=http://localhost:3000`을 사용한다.

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

## 3. 빌드

```bash
./gradlew clean build --no-daemon
```

현재 MVP 구현 단계에서는 테스트 코드를 작성하지 않는다. 위 명령은 컴파일과 실행 가능한 JAR 생성을 검증한다.

## 4. 확인 목록

- [ ] `./gradlew bootRun`으로 애플리케이션이 실행된다.
- [ ] Actuator health가 `UP`을 응답한다.
- [ ] Swagger UI와 OpenAPI JSON에 접근할 수 있다.
- [ ] `./gradlew clean build --no-daemon`이 성공한다.
