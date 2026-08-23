# MVP2 API 문서

- 상태: 구현 목표 v1

- [MVP2 API 계약](mvp2-api-contract.md): 다음 구현이 맞춰야 하는 endpoint와 데이터 변경
- 실제 구현 후 정본: `/v3/api-docs`의 OpenAPI JSON과 `/swagger-ui/index.html`
- 공통 규칙: [API 컨벤션](../conventions/api-convention.md)

구현 전에는 계약 문서가 목표이고 구현 후에는 생성 OpenAPI가 실제 정본이다. 계약 문서는 요구사항과 endpoint 대응, 중요한 불변식만 남기고 DTO 전체 복제는 피한다.
