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

예외 계층과 상태 코드 매핑은 [예외 컨벤션](../conventions/exception-convention.md)을 따른다.

## 엔드포인트 작성 양식

API마다 다음 내용을 구현 전에 작성한다.

| 항목 | 예시 |
| --- | --- |
| API 이름 | 매물 등록 |
| 목적 | 새로운 후보 매물을 등록한다 |
| Method | `POST` |
| URL | `/api/properties` |
| 인증 | 필요 |
| 요청 헤더 | 없음 |
| Path Parameter | 없음 |
| Query Parameter | 없음 |
| Request Body | 주소, 보증금, 월세 |
| 성공 상태 | `201 Created` |
| 성공 응답 | `PropertyResponse` |
| 대표 오류 | `PROPERTY_ALREADY_REGISTERED` |
| 멱등성·동시성 고려사항 | 동일 주소를 동시에 등록하면 하나만 성공한다 |
| 관련 Issue·요구사항 | |
| 비고 | 동일 주소 중복 등록 불가 |

양식을 복사해 API마다 하나씩 작성하고, Request와 Response 예시 JSON도 함께 작성한다.
