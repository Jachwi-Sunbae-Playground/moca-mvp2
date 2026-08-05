# 테스트와 빌드

모든 명령은 `backend`에서 실행한다.

## 테스트

```bash
./gradlew test
```

Docker가 실행 중이어야 한다. 로컬 Compose MySQL은 필요하지 않으며 Testcontainers가 임시 MySQL을 생성하고 정리한다.

## 전체 빌드

```bash
./gradlew clean build --no-daemon
```

PR을 올리기 전에 전체 빌드를 실행한다. GitHub Actions의 Backend CI도 Java 21과 Gradle Wrapper로 같은 명령을 실행한다.

## 확인 기준

- 컴파일과 모든 테스트가 성공한다.
- 통합 테스트가 로컬 MySQL이 아닌 Testcontainers MySQL에서 실행된다.
- 실패한 테스트를 비활성화하거나 삭제해 빌드를 통과시키지 않는다.
- 환경 문제로 실패했다면 원인과 해결 방법을 [문제 해결](troubleshooting.md)에 기록한다.
