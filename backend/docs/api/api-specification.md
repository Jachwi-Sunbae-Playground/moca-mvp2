# API 명세

- 상태: 초안
- 현재 등록된 비즈니스 API: 없음
- 구현 확인: Swagger UI `/swagger-ui/index.html`, OpenAPI JSON `/v3/api-docs`

## 공통 성공 응답

```json
{
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "data": {}
}
```

## 공통 오류 응답

```json
{
  "code": "MEMBER_NOT_FOUND",
  "message": "회원을 찾을 수 없습니다.",
  "errors": []
}
```

검증 오류의 `errors`에는 필드, 거절된 값과 사유를 기록한다. 민감정보는 포함하지 않는다.

## 엔드포인트 작성 양식

### `METHOD /api/resources`

- 목적:
- 인증·권한:
- 요청 헤더:
- Path/Query Parameter:
- 요청 본문:
- 성공 상태와 응답:
- 검증 오류:
- 비즈니스 오류:
- 멱등성·동시성 고려사항:
- 관련 Issue·요구사항:

API를 추가할 때 양식을 복사하고 실제 JSON 예시를 함께 작성한다.
