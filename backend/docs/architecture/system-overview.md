# 시스템 개요

- 상태: 초안
- 갱신 조건: 외부 시스템, 인증, 데이터베이스 또는 주요 데이터 흐름을 구현할 때
- 문서 성격: 파생
- 대조 대상: 실제 구성 요소, [배포 아키텍처 설계](../../../docs/operations/deployment-architecture.md)

## 현재 경계

```text
사용자
  ↓
프론트엔드(React SPA)
  ↓ HTTPS/JSON
백엔드(Spring Boot)
  ├─ Actuator health
  └─ Swagger/OpenAPI
```

- 백엔드는 기존 프로토타입 구현을 제거하고 Spring Boot 진입점만 남긴 상태다.
- 현재 Controller, 업무 도메인, 인증, 데이터베이스와 객체 저장소 연동은 구현하지 않았다.
- 데이터베이스 마이그레이션 도구는 사용하지 않는다.
- API 계약은 별도 Markdown 문서로 관리하지 않고 구현에서 생성되는 Swagger/OpenAPI로 확인한다.
- 배포 인프라는 EC2, ALB와 CodePipeline 기반 구성을 유지한다. 구체적인 값과 절차는 [배포](../operations/deployment.md)에서 관리한다.

새 구성 요소를 구현할 때 책임, 통신 방식, 실패 영향과 소유 팀을 함께 기록한다.
