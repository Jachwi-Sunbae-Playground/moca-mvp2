# 시스템 개요

- 상태: MVP2 구현·공개 환경 운영 중
- 문서 성격: 파생
- 대조 대상: 실제 백엔드·프론트엔드 구성 요소, [MVP2 배포 아키텍처](../../../docs/operations/mvp2-deployment-architecture.md)

## 현재 경계

```text
사용자 브라우저
  ↓
React SPA
  ↓ JSON + Bearer Access Token
Spring Boot
  ├─ demo 또는 Google OAuth 인증 adapter
  ├─ 회원·매물·사진·메모·체크리스트 API
  ├─ demo 또는 Kakao Local 지도 adapter
  ├─ Spring JDBC → MySQL 8.4
  ├─ S3 API → 로컬 MinIO 또는 운영 비공개 S3
  ├─ Actuator health/info
  └─ Swagger/OpenAPI
```

로컬 기본은 외부 키가 없는 `demo` 인증·지도와 MinIO다. `live`에서는 프론트가 Google Authorization Code + PKCE 로그인을 시작하고 백엔드가 Google 토큰 교환과 ID token 검증을 수행한 뒤 자체 JWT Access Token을 발급한다. 지도는 프론트 Kakao Maps JavaScript SDK와 백엔드 Kakao Local REST API를 사용한다.

회원 ID를 기준으로 주소·좌표를 포함한 매물, 구조화 메모와 단계별 체크리스트 스냅샷을 MySQL에 저장한다. 사진 메타데이터는 DB, 바이트는 비공개 객체 저장소에 두고 소유자 검증 백엔드 endpoint로만 조회한다. DB 스키마 정본은 [데이터베이스 초기화](../guides/database-initialization.md)의 단일 SQL이다.

API의 실행 계약은 구현에서 생성되는 Swagger/OpenAPI를 우선 확인한다. 제품 요구사항은 [MVP2 기능 명세](../../../docs/product/specs/README.md), 스키마 설명은 [MVP2 데이터 모델](mvp2-data-model.md), 외부 지도 전환은 [지도 연동](../guides/map-integration.md)을 따른다.

운영은 `jachwisunbae.shop`의 단일 EC2에서 Caddy가 HTTPS와 SPA를 제공하고 `/api`를 Spring Boot로 전달한다. MySQL은 같은 인스턴스의 비공개 Docker 네트워크, 사진은 별도 비공개 S3에 두며 GitHub Actions가 OIDC와 SSM으로 수동 배포한다. 상세 구성과 운영 제한은 [MVP2 배포 아키텍처](../../../docs/operations/mvp2-deployment-architecture.md)를 따른다.
