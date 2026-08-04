# API 컨벤션

## 설계와 문서화

- 구현 전에 [API 명세](../api/api-specification.md)에 경로, 요청, 응답과 대표 오류를 합의한다.
- 구현 후 Swagger/OpenAPI에서 실제 계약을 확인한다.
- 사전 명세와 구현이 다르면 같은 PR에서 코드 또는 문서를 수정한다.

## URL과 상태 코드

- URL은 복수형 리소스 명사를 사용하고 불필요한 행위 동사를 넣지 않는다.
- 생성은 `201 Created`, 일반 성공은 `200 OK`, 응답 본문 없는 삭제는 `204 No Content`를 기본으로 한다.
- 요청 형식 오류는 `400`, 인증 실패는 `401`, 권한 부족은 `403`, 리소스 없음은 `404`, 상태 충돌은 `409`를 기본으로 검토한다.

## 요청과 응답

- API마다 전용 Request/Response DTO를 사용하고 Domain 객체를 직접 노출하지 않는다.
- 성공 응답은 `{ code, message, data }`를 기본으로 한다. `204` 응답에는 본문을 넣지 않는다.
- 오류 응답은 `{ code, message, errors }`로 통일한다.
- `errors`는 입력 검증 실패의 필드별 상세에 사용하고 일반 비즈니스 오류에서는 빈 배열을 사용한다.
- 오류 코드는 `MEMBER_NOT_FOUND`처럼 `도메인_상태` 형식의 UPPER_SNAKE_CASE를 사용한다.

## 계층 책임

- Service는 HTTP 상태, `ResponseEntity`와 Controller DTO를 알지 않는다.
- Service는 의미 있는 커스텀 예외를 발생시키고 전역 예외 처리기가 HTTP 상태와 공통 오류 응답으로 변환한다.
- Request DTO는 형식과 필수값, Service는 권한·중복·상태 전이 같은 비즈니스 규칙을 검증한다.
