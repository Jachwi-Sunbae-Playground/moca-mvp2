# 테스트 전략

테스트 종류별 작성 기준을 정한다. 공통 규칙은 [테스트 컨벤션](test-convention.md)을 따른다.

## Domain 단위 테스트

Domain 단위 테스트는 Spring과 외부 시스템 없이 순수 Java 객체로 실행한다.

- `@SpringBootTest`를 사용하지 않는다.
- `@ExtendWith(MockitoExtension.class)`를 기본으로 사용하지 않는다.
- Domain 객체는 `new` 또는 정적 팩터리 메서드로 생성한다.
- Domain 객체를 Mock으로 만들지 않는다.
- 비즈니스 규칙의 정상·예외·경계값을 검증한다.
- 외부 상태 없이 같은 결과가 반복되어야 한다.
- Repository와 DB의 동작은 검증하지 않는다.

### 주요 검증 대상

- 생성 불변 조건
- 금액과 날짜의 경계값
- 상태 변경 규칙
- 권한 판단
- 지원 자격 판단
- 매물 비교 규칙
- 계약 전 확인 규칙

## Service 단위 테스트

기본은 실제 DB를 사용하는 통합 테스트다. Service 단위 테스트는 Service의 분기와 협력 방식 자체가 중요한 경우에만 예외적으로 작성한다.

### 사용하는 경우

- Repository 응답에 따른 Service 분기를 검증할 때
- 외부 API 응답에 따른 후속 행동을 검증할 때
- 특정 조건에서 저장이나 외부 호출을 생략해야 할 때
- 실패 시 다른 협력이 호출되지 않아야 할 때
- 실제 DB 없이 Service 책임만 빠르게 검증할 가치가 있을 때

### 사용하지 않는 경우

- 실제 SQL의 정확성을 확인하려는 경우
- 트랜잭션과 롤백을 검증하려는 경우
- DB UNIQUE 제약을 검증하려는 경우
- 동시성을 검증하려는 경우
- Service가 단순히 Repository 메서드를 순서대로 호출하는 경우
- Mock 설정이 테스트 본문 대부분을 차지하는 경우

Mock 기반 Service 테스트는 다음 내용만 보장한다.

> Repository가 정해진 값을 반환하면 Service가 올바르게 반응하는가?

다음 내용은 보장하지 않는다.

- Repository의 SQL이 정확한가?
- 컬럼명과 Parameter Binding이 정확한가?
- RowMapper가 올바르게 동작하는가?
- DB 제약조건이 동작하는가?
- 트랜잭션이 실제로 적용되는가?

따라서 Mock으로 가정한 Repository의 핵심 동작은 실제 DB 기반 Repository 테스트로 검증한다.

## Repository 슬라이스 테스트

순수 `JdbcTemplate`을 사용하므로 Repository 테스트에는 `@JdbcTest`를 사용한다. 공통 설정은 `RepositoryTest`를 상속해 받는다.

```java
class PropertyJdbcRepositoryTest extends RepositoryTest {
}
```

`RepositoryTest`는 `@JdbcTest`와 `@AutoConfigureTestDatabase(replace = Replace.NONE)`, Testcontainers MySQL 연결을 함께 제공한다. `@JdbcTest`는 기본적으로 DataSource를 임베디드 DB로 교체하므로 `replace = NONE`이 없으면 [ADR-0004](../adr/0004-manage-local-development-environment.md)가 정한 Testcontainers MySQL로 실행되지 않는다. 개별 테스트에 애너테이션을 직접 조합하지 않는다.

### 직접 작성한 핵심 SQL을 검증한다

`JdbcTemplate`에서는 SQL과 RowMapper를 직접 작성하므로 Repository 테스트의 가치가 크다.

다음 내용을 검증한다.

- SQL 문법
- 컬럼명
- Parameter Binding
- RowMapper
- Insert 후 생성 ID
- 결과가 없을 때 `Optional.empty()`
- 목록 결과가 없을 때 빈 List
- 정렬
- Filtering
- Pagination
- DB UNIQUE와 FK 제약
- 날짜와 시간 자료형
- 여러 조건 중 하나만 다른 경계값
- 반복 Query 발생 여부

Service 통합 테스트가 Repository를 사용하더라도 복잡한 SQL의 경계 조건까지 검증하지 않는다면 Repository 테스트를 별도로 작성한다.

## 통합 테스트

여러 계층과 실제 DB가 협력해 Use Case가 끝까지 동작하는지 검증할 때 `@SpringBootTest`를 사용한다. 공통 설정은 `IntegrationTest`를 상속해 받는다.

기본 `@SpringBootTest`는 Application Context를 로딩하지만 실제 서버를 시작하지 않는다. 실제 서버가 필요하면 `RANDOM_PORT`를 사용한다.

### 검증 대상

- Spring Bean 연결
- Service와 Repository 협력
- Command·Result 변환
- 실제 SQL과 DB 상태 변화
- Service 트랜잭션
- 여러 Repository가 참여하는 Use Case
- 외부 Client와의 통합

### 작성 기준

- 모든 Domain 경계값을 통합 테스트에서 반복하지 않는다.
- 핵심 Use Case의 정상 흐름과 대표 실패 흐름을 검증한다.
- 외부 API는 실제 운영 서버 대신 Stub Server를 사용한다.
- 내부 Repository를 전부 Mock으로 교체하지 않는다.
- Mock을 많이 사용한다면 해당 테스트가 통합 테스트인지 다시 검토한다.

## 트랜잭션 테스트

트랜잭션은 `@Transactional` 애너테이션의 존재가 아니라 실제 롤백 결과로 검증한다.

### 필수 규칙

- 트랜잭션을 검증하는 테스트에는 테스트 메서드의 `@Transactional`을 사용하지 않는다.
- 테스트 자체의 트랜잭션이 Service 트랜잭션을 감추지 않도록 한다.
- 중간 실패를 발생시킨 뒤 DB 상태가 전부 원복됐는지 확인한다.
- `new Service(...)`로 만든 객체에서는 Spring AOP의 `@Transactional`이 적용되지 않는다는 점을 인지한다.
- 트랜잭션 테스트는 Spring Context가 있는 실제 Service Bean으로 수행한다.
- 트랜잭션 전파, 격리 수준과 동시성은 Mock으로 검증하지 않는다.

테스트에 `@Transactional`을 사용한 자동 롤백은 데이터 정리에는 편리하지만 다음 검증에서는 사용하지 않는다.

- Service 트랜잭션 경계
- 중간 실패 시 롤백
- 실제 Commit 시점의 동작
- 동시성
- 여러 Thread 또는 실제 HTTP 요청

## 인수 테스트

인수 테스트는 사용자 관점의 핵심 시나리오를 검증한다. `AcceptanceTest`를 상속하면 실제 서버와 `acceptance` 태그를 함께 받는다.

### 작성 대상

1차 MVP 대상이며 기능 구현 시점에 갱신한다.

- 회원가입 후 로그인
- 지원 조건 입력 후 확인 가능한 정책 조회
- 매물 등록 후 상세 조회
- 매물 방문 기록 생성 후 비교
- 계약 전 확인 항목 생성

### 작성 기준

- 기술적인 클래스명보다 사용자 행동의 언어를 사용한다.
- 핵심 정상 시나리오와 중요한 실패 시나리오만 작성한다.
- 모든 Validation과 Domain 경계값을 인수 테스트에서 반복하지 않는다.
- HTTP 상태, Response Body, 오류 코드를 검증한다.
- 구현 내부의 Service나 Repository 호출 여부를 검증하지 않는다.
