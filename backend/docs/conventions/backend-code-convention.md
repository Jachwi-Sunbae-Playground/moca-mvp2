# 백엔드 코드 컨벤션

## 핵심 원칙

- 패키지 구조, 의존 방향과 패키지별 책임은 [백엔드 패키지 구조](../architecture/backend-package-structure.md)를 따른다.
- 1차 MVP는 `JdbcTemplate`과 레이어드 구조를 사용하며 JPA와 헥사고날 아키텍처는 도메인 복잡도가 확인될 때 재검토한다.
- 실제 기능이 생길 때만 패키지와 추상화를 추가한다.

## DTO

- Controller DTO는 HTTP 입력 검증과 응답 형식을 담당한다.
- Command와 Result는 서비스의 공개 유스케이스 경계를 분리할 필요가 있을 때 사용한다.
- 구조를 맞추기 위해 내용이 같은 DTO를 기계적으로 복제하지 않는다.
- 도메인 객체와 Repository 객체의 별도 분리는 변환 비용을 감수할 근거가 생겼을 때 결정한다.

## 작성 규칙

- Entity나 도메인 객체를 API 응답으로 직접 노출하지 않는다.
- 도메인 객체는 의미 있는 생성·상태 변경 메서드를 사용하고 public setter를 두지 않는다.
- Lombok은 사용하지 않는다.
- 클래스는 명사형 UpperCamelCase, 메서드는 동사형 lowerCamelCase, 상수는 UPPER_SNAKE_CASE를 사용한다.
- `Manager`, `Processor`, `Util`처럼 책임이 모호한 이름보다 업무 의미가 드러나는 이름을 사용한다.
- wildcard import를 사용하지 않고 [Java 코드 스타일 ADR](../adr/0005-select-java-code-style.md)을 따른다.
