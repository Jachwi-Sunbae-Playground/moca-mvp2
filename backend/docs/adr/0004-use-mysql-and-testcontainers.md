# ADR-0004: 로컬과 테스트 데이터베이스 환경을 구성한다

- 상태: 승인
- 결정일: 2026-08-04
- 참여자: 자취선배 백엔드 팀

## 맥락

팀원이 각자 다른 MySQL 설치와 설정을 사용하면 재현하기 어려운 문제가 생길 수 있다. JDBC Template은 실제 SQL과 데이터베이스 동작의 영향을 직접 받으므로 H2나 Mock만으로는 MySQL 호환성을 충분히 검증할 수 없다.

## 결정

- 로컬 데이터베이스는 Docker Compose로 실행하는 MySQL `8.4.10`을 사용한다.
- 로컬 기본값을 제공하되 모든 연결 정보는 환경변수로 변경할 수 있게 한다.
- 테스트는 H2 대신 Testcontainers의 MySQL `8.4.10`을 사용한다.
- 공통 `IntegrationTest`에서 Spring Boot `@ServiceConnection`으로 임시 DB 연결 정보를 주입한다.
- 테스트는 `test` 프로필을 사용하며 로컬 MySQL 설정에 의존하지 않는다.
- 최소 연결 테스트는 실제 `SELECT 1` 쿼리로 검증한다.

## 근거

- Docker 이미지 버전을 고정하면 팀원의 OS와 기존 MySQL 설치 상태에 덜 의존한다.
- 로컬과 테스트가 같은 MySQL 버전을 사용해 SQL 문법과 드라이버 차이를 줄인다.
- Testcontainers가 테스트마다 격리된 환경을 제공하고 종료 후 자동으로 정리한다.
- Mock Repository는 호출 여부만 검증할 뿐 실제 SQL의 정확성을 보장하지 못한다.

## 검토한 대안

| 대안 | 장점 | 우려 | 선택하지 않은 이유 |
| --- | --- | --- | --- |
| 팀원 PC에 MySQL 직접 설치 | 컨테이너 실행 비용이 없다 | 버전, 계정, 문자셋, 기존 데이터가 서로 다를 수 있다 | 재현성과 초기화 방법을 통일하기 어렵다 |
| H2 테스트 | 빠르고 설정이 단순하다 | MySQL과 SQL 문법, 함수, 제약조건 동작이 다르다 | JDBC SQL을 검증하는 신뢰도가 낮다 |
| Repository Mock 중심 테스트 | 매우 빠르고 실패 원인을 좁히기 쉽다 | 실제 쿼리와 매핑 오류를 발견하지 못한다 | 데이터 접근 검증을 대체할 수 없다 |
| 공유 개발 DB | 팀원이 별도 DB를 실행하지 않아도 된다 | 네트워크, 공유 상태, 동시 테스트에 의존한다 | 테스트 격리와 오프라인 개발이 어렵다 |

## 결과와 트레이드오프

### 기대하는 결과

- 새 팀원이 같은 명령으로 동일한 MySQL을 실행한다.
- MySQL 전용 SQL과 매핑 오류를 PR 이전에 발견한다.
- 테스트 데이터가 개인 로컬 DB를 오염시키지 않는다.

### 감수하는 비용과 한계

- 개발과 통합 테스트에 Docker가 필수다.
- 최초 이미지 다운로드와 컨테이너 시작 때문에 테스트가 느려진다.
- Docker Desktop 자원 사용량이 증가한다.
- Compose는 `.env`를 자동으로 읽지만 Spring 프로세스에는 별도로 환경변수를 전달해야 한다.
- 데이터베이스 스키마 마이그레이션 도구는 아직 결정하지 않았다.

## 검증 방법

- `docker compose ps`에서 MySQL이 `healthy`인지 확인한다.
- Actuator health가 `UP`인지 확인한다.
- Docker만 실행하고 Compose MySQL은 중지한 상태에서 `./gradlew test`가 성공하는지 확인한다.

## 재검토 조건

- 테스트 실행 시간이 개발 피드백을 유의미하게 늦춘다.
- CI 또는 팀 개발 환경에서 Docker 사용이 제한된다.
- 운영 DB가 MySQL이 아닌 다른 제품으로 결정된다.
- 스키마 변경이 시작되어 버전 관리와 마이그레이션 자동화가 필요하다.

## 참고 자료

- [Spring Boot Testcontainers와 Service Connection](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html)
