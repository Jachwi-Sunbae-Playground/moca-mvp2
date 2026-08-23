# MVP2 API 문서

- 상태: 구현 완료 v1
- 문서 성격: 파생
- 대조 대상: 실제 Spring MVC 컨트롤러와 `/v3/api-docs`

- [MVP2 API 계약](mvp2-api-contract.md): 요구사항과 endpoint 대응 및 핵심 불변식 요약
- 실제 실행 정본: `/v3/api-docs`의 OpenAPI JSON과 `/swagger-ui/index.html`
- 공통 규칙: [API 컨벤션](../conventions/api-convention.md)

생성 OpenAPI가 실제 실행 정본이다. 계약 문서는 요구사항과 endpoint 대응, 중요한 불변식만 남기고 DTO 전체 복제는 피한다.
