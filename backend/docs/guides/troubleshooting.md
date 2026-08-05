# 문제 해결

## Docker에 연결할 수 없다

Docker Desktop 또는 Docker Engine이 실행 중인지 확인한다. 로컬 MySQL과 Testcontainers 테스트 모두 Docker가 필요하다.

## 3306 포트를 이미 사용 중이다

`backend/.env`에서 포트를 변경하고 같은 환경변수를 Spring 프로세스에도 전달한다.

```bash
DB_PORT=3307 docker compose up -d mysql
DB_PORT=3307 ./gradlew bootRun
```

## Java 버전이 다르다

```bash
./gradlew --version
```

JVM이 Java 21이 아니면 `JAVA_HOME`과 IntelliJ의 Gradle JVM을 Java 21로 변경한다.

## `.env` 값을 변경했는데 애플리케이션에 반영되지 않는다

Compose는 `.env`를 자동으로 읽지만 Spring Boot는 자동으로 읽지 않는다. [환경변수 가이드](environment-variables.md)에 따라 프로세스 환경변수로 전달한다.

## IntelliJ 코드 스타일이 다르다

`Settings > Editor > Code Style > Java > Import Scheme`에서 `backend/config/code-style/intellij-java-wooteco-style.xml`을 불러오고 `WootecoStyle`을 선택한다.

## 해결되지 않는 문제 기록

재현 명령, 기대 결과, 실제 결과, OS·Java·Docker 버전과 해결 시도를 이 문서 또는 관련 이슈에 남긴다. 비밀번호와 토큰은 포함하지 않는다.
