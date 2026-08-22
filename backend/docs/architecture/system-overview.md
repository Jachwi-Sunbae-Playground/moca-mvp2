# 시스템 개요

- 상태: MVP1 기준
- 문서 성격: 파생
- 대조 대상: 실제 백엔드·프론트엔드 구성 요소, [MVP2 배포 아키텍처](../../../docs/operations/mvp2-deployment-architecture.md)

## 현재 경계

```text
사용자 브라우저
  ↓
React SPA
  ↓ JSON + Bearer Access Token
Spring Boot
  ├─ Google OAuth code 교환·ID token 검증
  ├─ 회원·매물·메모·체크리스트 API
  ├─ Spring JDBC → MySQL 8.4
  ├─ Actuator health/info
  └─ Swagger/OpenAPI

사진 S3 저장·조회 구현: MVP2 후속 범위
지도·공공데이터 연동: MVP2 후속 범위
```

프론트엔드는 Google Authorization Code + PKCE 로그인을 시작하고, 백엔드가 Google 토큰 교환과 ID token 검증을 수행한 뒤 자체 JWT Access Token을 발급한다. 이후 회원 ID를 기준으로 매물, 구조화 메모와 단계별 체크리스트 데이터를 MySQL에 저장한다.

DB 스키마 정본은 [데이터베이스 초기화](../guides/database-initialization.md)의 단일 SQL이다. 현재 코드에는 사진 메타데이터와 AWS S3 SDK 설정이 있지만 실제 객체 저장·조회 경계는 완성되지 않았다. 문서나 화면 계약만 보고 구현 완료로 간주하지 않는다.

API의 실행 계약은 구현에서 생성되는 Swagger/OpenAPI를 우선 확인한다. MVP1 요구사항·정책·ERD·API 명세를 사람이 읽는 문서로 통합하는 작업은 [MVP2 전환 기준](../../../docs/operations/mvp2-transition.md)의 후속 범위다.

현재는 로컬에서만 개발한다. 단일 EC2 배포 파일은 준비했지만 AWS 리소스와 공개 환경은 아직 없다.
