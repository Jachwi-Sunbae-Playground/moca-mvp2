# 백엔드 패키지 구조

## 기본 구조

기술 레이어보다 업무 도메인을 먼저 나누고 각 도메인 안에서 레이어를 구분한다.

```text
com.jachwisunbae
├── {domain}
│   ├── controller
│   ├── service
│   ├── repository
│   ├── domain
│   └── dto
└── common
```

## 의존 방향

```text
controller → service → repository
                 ↓
               domain
```

- Controller는 HTTP 요청과 응답 변환을 담당한다.
- Service는 유스케이스 실행, 비즈니스 검증과 트랜잭션을 담당한다.
- Repository는 `JdbcTemplate`을 이용한 데이터 접근을 담당한다.
- Domain은 업무 개념, 상태와 불변식을 표현한다.
- `common`에는 둘 이상의 도메인에서 실제로 공유하는 기술 기능만 둔다.

세부 작성 규칙은 [백엔드 코드 컨벤션](../conventions/backend-code-convention.md), 선택 근거는 [ADR-0003](../adr/0003-select-database-and-persistence.md)을 따른다.
