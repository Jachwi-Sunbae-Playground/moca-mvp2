# 백엔드 로컬 개발 환경 구성 및 실행

## 1. 준비물

| 항목 | 기준 |
| --- | --- |
| JDK | Java 21 |
| Docker | Docker Engine과 Docker Compose를 사용할 수 있는 환경 |
| Git | GitHub 저장소를 복제할 수 있는 버전 |

Gradle과 MySQL은 따로 설치하지 않는다. Gradle Wrapper와 Docker Compose가 팀이 사용하는 버전을 준비한다.

```bash
git clone https://github.com/woowacourse-teams/2026-jachwi-sunbae.git
cd 2026-jachwi-sunbae
java -version
docker --version
docker compose version
```

## 2. 로컬 MySQL 실행

저장소 루트에서 실행한다. 기본 설정은 별도의 `.env`가 없어도 동작한다.

```bash
docker compose up -d mysql
docker compose ps
```

`mysql` 상태가 `healthy`면 준비가 완료된 것이다.

## 3. 백엔드 실행

저장소 루트에서 실행한다.

```bash
cd backend
./gradlew bootRun
```

실행 후 다음 주소를 확인한다.

| 확인 항목 | 주소 |
| --- | --- |
| 서버 상태 | `http://localhost:8080/actuator/health` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |

`/actuator/health`의 응답이 `{"status":"UP"}`이면 애플리케이션과 MySQL이 정상적으로 연결된 것이다.

## 4. 테스트와 빌드

Docker가 실행 중인 상태에서 새 터미널을 열고 저장소 루트에서 실행한다. Compose의 MySQL은 필요하지 않으며 Testcontainers가 임시 MySQL을 자동으로 생성하고 정리한다.

```bash
cd backend
./gradlew test
./gradlew clean build
```

## 5. 환경변수 변경

기본값을 변경해야 할 때만 저장소 루트에 `.env`를 생성한다. `.env`는 Git에 커밋하지 않는다.

```bash
cp .env.example .env
```

| 환경변수 | 기본값 | 용도 |
| --- | --- | --- |
| `DB_HOST` | `localhost` | MySQL 호스트 |
| `DB_PORT` | `3306` | MySQL 포트 |
| `DB_NAME` | `jachwi_sunbae` | 데이터베이스 이름 |
| `DB_USERNAME` | `jachwi_sunbae` | 애플리케이션 계정 |
| `DB_PASSWORD` | `local_password` | 애플리케이션 계정 비밀번호 |
| `DB_ROOT_PASSWORD` | `local_root_password` | 로컬 MySQL root 비밀번호 |

Docker Compose는 루트의 `.env`를 자동으로 읽는다. 백엔드도 같은 값을 사용하려면 실행 프로세스에 환경변수를 전달해야 한다.

```bash
set -a
source .env
set +a
cd backend
./gradlew bootRun
```

## 6. 종료와 초기화

루트 디렉터리에서 MySQL을 종료한다. 기본 종료는 데이터 볼륨을 유지한다.

```bash
docker compose down
```

로컬 데이터까지 모두 초기화해야 할 때만 다음 명령을 사용한다.

> 주의: 다음 명령은 로컬 MySQL 데이터를 모두 삭제한다.

```bash
docker compose down --volumes
```

## 7. IntelliJ 코드 스타일

1. `Settings > Editor > Code Style > Java`로 이동한다.
2. 설정 아이콘에서 `Import Scheme > IntelliJ IDEA code style XML`을 선택한다.
3. `config/code-style/intellij-java-wooteco-style.xml`을 불러온다.
4. 스타일 이름 `WootecoStyle`을 선택한다.

`.editorconfig`의 공통 설정은 IntelliJ에서 자동으로 적용된다.

## 8. 주요 문제 해결

### Docker에 연결할 수 없다

Docker Desktop 또는 Docker Engine이 실행 중인지 확인한다. Testcontainers 테스트도 Docker가 필요하다.

### 3306 포트를 이미 사용 중이다

Compose와 백엔드에 같은 포트를 전달한다.

```bash
DB_PORT=3307 docker compose up -d mysql
cd backend
DB_PORT=3307 ./gradlew bootRun
```

### Java 버전이 다르다

저장소 루트에서 확인한다.

```bash
cd backend
./gradlew --version
```

JVM이 Java 21이 아니면 `JAVA_HOME`과 IntelliJ의 Gradle JVM을 Java 21로 변경한다.

## 9. 재현 완료 체크리스트

- [ ] `docker compose ps`에서 MySQL이 `healthy`다.
- [ ] `./gradlew bootRun`으로 애플리케이션이 실행된다.
- [ ] `/actuator/health`가 `UP`을 응답한다.
- [ ] Swagger UI에 접근할 수 있다.
- [ ] `./gradlew clean build`가 성공한다.
