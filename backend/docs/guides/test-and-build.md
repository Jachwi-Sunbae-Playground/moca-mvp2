# 테스트와 빌드

모든 명령은 `backend`에서 실행한다.

## 테스트

```bash
./gradlew test
```

Docker가 실행 중이어야 한다. 로컬 Compose MySQL은 필요하지 않으며 Testcontainers가 임시 MySQL을 생성하고 정리한다.

`./gradlew test`는 `acceptance` 태그가 붙은 인수 테스트를 제외한다. 인수 테스트까지 실행하려면 다음을 사용한다.

```bash
./gradlew testAll
```

테스트 종류별 작성 기준은 [테스트 전략](../conventions/test-strategy.md)을 따른다.

## 전체 빌드

```bash
./gradlew clean build --no-daemon
```

PR을 올리기 전에 전체 빌드를 실행한다. GitHub Actions의 Backend CI도 Java 21과 Gradle Wrapper로 같은 명령을 실행한다.

## 확인 기준

- 컴파일과 모든 테스트가 성공한다.
- 통합 테스트가 로컬 MySQL이 아닌 Testcontainers MySQL에서 실행된다.
- 실패한 테스트를 비활성화하거나 삭제해 빌드를 통과시키지 않는다.
- 환경 문제로 실패했다면 재현 명령, 기대 결과, 실제 결과와 OS·Java·Docker 버전을 관련 이슈에 기록한다. 비밀번호와 토큰은 포함하지 않는다.
