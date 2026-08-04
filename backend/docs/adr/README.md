# Architecture Decision Records

ADR은 프로젝트에 영향을 주는 기술적 결정을 당시의 맥락과 함께 보존한다. 결론뿐 아니라 선택 기준, 검토한 대안, 감수한 비용, 재검토 조건을 기록한다.

## 작성 규칙

1. 파일명은 `NNNN-title.md` 형식을 사용한다.
2. 하나의 ADR은 하나의 중심 결정을 다룬다.
3. 상태는 `제안`, `승인`, `폐기`, `대체` 중 하나로 표시한다.
4. 승인된 ADR의 결론을 바꿀 때는 기존 문서를 지우지 않고 새로운 ADR에서 대체한다.
5. 구현 결과나 전제가 달라지면 결과와 재검토 조건을 갱신한다.

새 ADR은 [ADR 템플릿](template.md)을 복사해 작성한다.

## 현재 ADR

| 번호 | 제목 | 상태 | 결정일 |
| --- | --- | --- | --- |
| [0001](0001-use-monorepo.md) | 모노레포를 사용한다 | 승인 | 2026-08-04 |
| [0002](0002-backend-runtime-and-build-tools.md) | 백엔드 런타임과 빌드 도구를 선택한다 | 승인 | 2026-08-04 |
| [0003](0003-use-layered-architecture-and-jdbc-template-for-mvp.md) | MVP에 레이어드 구조와 JDBC Template을 사용한다 | 승인 | 2026-08-04 |
| [0004](0004-use-mysql-and-testcontainers.md) | 로컬과 테스트 데이터베이스 환경을 구성한다 | 승인 | 2026-08-04 |
| [0005](0005-use-wooteco-style-and-github-actions.md) | 코드 스타일과 CI 검증 방식을 선택한다 | 승인 | 2026-08-04 |
