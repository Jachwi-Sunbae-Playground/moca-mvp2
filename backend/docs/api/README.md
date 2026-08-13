# API 문서

## 문서 역할

- 프론트엔드는 [API 명세의 v1.1 인계 기준선](api-specification.md#프론트엔드-v11-인계-기준선)과 [27개 구현 추적표](api-specification.md#구현-추적)를 계약 정본으로 사용한다.
- 구현 후에는 Swagger UI와 `/v3/api-docs`에서 배포된 실제 계약과 `info.version`을 확인한다.
- 사전 명세와 OpenAPI 결과가 다르면 코드 또는 문서를 같은 PR에서 수정한다.

## 설계 시점

API는 기능 구현 전에 설계한다.

```text
기능 요구사항 확정
→ API 설계서 작성
→ 프론트엔드와 검토
→ 백엔드 구현
→ Swagger 동기화
```

- 초기 설계는 Notion으로 작성하고, 합의된 계약은 [API 명세](api-specification.md)에 옮긴다.
- Swagger는 구현 결과물이 아니라 API 계약 확인 용도로 사용한다.
- API가 변경되면 구현 코드와 Swagger 문서를 함께 수정한다.

## 변경 절차

1. 사용자 흐름과 필요한 API를 정의한다.
2. 구현 전에 API 명세에 요청·응답 예시와 오류를 작성한다.
3. 프론트엔드와 백엔드가 명세를 리뷰한다.
4. 구현과 테스트 후 Swagger/OpenAPI 결과를 확인한다.
5. 변경 사항과 호환성 영향을 PR에 기록한다.

세부 형식은 [API 컨벤션](../conventions/api-convention.md)을 따른다.
