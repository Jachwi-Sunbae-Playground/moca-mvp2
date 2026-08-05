# API 문서

## 문서 역할

- 구현 전에는 [API 명세](api-specification.md)로 프론트엔드와 경로, 요청, 응답과 오류를 합의한다.
- 구현 후에는 Swagger UI와 `/v3/api-docs`에서 실제 계약을 확인한다.
- 사전 명세와 OpenAPI 결과가 다르면 코드 또는 문서를 같은 PR에서 수정한다.

## 변경 절차

1. 사용자 흐름과 필요한 API를 정의한다.
2. 구현 전에 API 명세에 요청·응답 예시와 오류를 작성한다.
3. 프론트엔드와 백엔드가 명세를 리뷰한다.
4. 구현과 테스트 후 Swagger/OpenAPI 결과를 확인한다.
5. 변경 사항과 호환성 영향을 PR에 기록한다.

세부 형식은 [API 컨벤션](../conventions/api-convention.md)을 따른다.
