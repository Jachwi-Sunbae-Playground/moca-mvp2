# 다음 작업용 MVP2 단일 구현 프롬프트

- 상태: 사용 가능 v1
- 대조 대상: [`mvp2-implementation-brief.md`](mvp2-implementation-brief.md)

아래 본문을 새 Codex 작업에 그대로 입력합니다.

---

`Jachwi-Sunbae-Playground/moca-mvp2` 저장소에서 MVP2 전체를 구현해줘.

작업 시작 전에 저장소의 `AGENTS.md`와 아래 문서를 모두 읽고 정본 순서를 지켜줘.

- `docs/product/mvp2-implementation-brief.md`
- `docs/product/scope/mvp1-baseline.md`
- `docs/product/scope/mvp2-scope.md`
- `docs/product/scope/mvp1-to-mvp2-changes.md`
- `docs/product/specs/README.md`와 연결된 모든 기능 명세
- `docs/product/flows/mvp2-user-flow.md`
- `docs/product/decisions/README.md`
- `docs/design/wireframes/README.md`
- `docs/design/wireframes/mvp2-moca.pen`
- `backend/docs/architecture/mvp2-data-model.md`
- `backend/docs/api/mvp2-api-contract.md`
- `backend/docs/guides/map-integration.md`
- `docs/operations/mvp2-preview-and-deployment-readiness.md`

목표는 기획을 다시 완벽하게 만드는 것이 아니라, 사용자가 와이어프레임의 MVP2 전체를 로컬에서 실제로 실행하고 사용할 수 있게 하는 것이다. 기존 화면 개편뿐 아니라 `13-1`, `13-2`, `13-3` 지도 기능도 완료 범위다.

다음 원칙으로 자율적으로 진행해줘.

1. GitHub 추적 Issue를 만들고 `main`에서 `codex/` 작업 브랜치를 분기한다. `main`에 직접 작업하지 않는다.
2. 현재 코드·테스트·스키마를 먼저 감사하고 MVP1 회귀 기준을 잡은 뒤 구현한다.
3. 비차단적인 정책·UI·기술 선택은 질문 때문에 멈추지 말고 가장 단순하고 안전한 선택을 한다. 새 결정은 `docs/product/decisions/README.md`에 이유·영향과 함께 기록한다.
4. 비용 발생, 유료 API·비즈월렛 활성화, 실제 AWS 자원 생성, DNS 변경, 외부 배포, 복구하기 어려운 삭제만 사용자 확인 없이 실행하지 않는다.
5. 로컬 기본은 외부 key가 필요 없는 `demo` 모드로 만든다. 데모 로그인, 데모 지도·주소·주변 시설과 MinIO를 사용해 전체 흐름을 검증할 수 있어야 한다.
6. `live` 모드는 Google OAuth, Kakao Maps JavaScript SDK, 백엔드 Kakao Local REST API, 운영 S3로 전환할 수 있게 adapter와 환경변수를 구현한다. 비밀값을 커밋하지 않는다.
7. MVP1에서 미구현인 실제 사진 업로드와 인증 콘텐츠 조회, 객체 저장 보상 삭제를 완성한다.
8. 주소·도로명/지번·위도·경도·최근 활동 시각, 대표 사진 유일성, cascade 정책을 목표 데이터 모델대로 반영한다. Flyway를 다시 도입하지 않는다.
9. 기존 기능은 `00`부터 `10`까지 와이어프레임 순서로 개편하고, 마지막에 지도 `13-1 → 13-2 → 02`와 `13-1 → 13-3` 흐름을 완성한다.
10. 화면은 제공 PNG와 `.pen`의 구조·문구·흐름을 따르되 모바일 390px뿐 아니라 일반 모바일 폭에서도 깨지지 않게 한다. 공통 token과 component를 재사용한다.
11. 정상 상태만 만들지 말고 문서의 7개 빈 상태, 로딩, 저장 중, 오류, 재시도와 접근성을 구현한다.
12. 상태·메모 자동 저장, 소유자 검증, 체크리스트 스냅샷·교체와 진행 집계는 회귀시키지 않는다.
13. 구현과 함께 관련 문서, `.env.example`, 환경변수 가이드, OpenAPI 설명, 로컬 실행 안내를 갱신한다. 시점 고정 문서는 수정하지 않는다.
14. 백엔드 테스트, 프론트엔드 테스트·lint·build, `python3 .github/scripts/check_docs.py`를 통과시킨다.
15. 로컬 애플리케이션을 직접 실행하고 브라우저 모바일 viewport에서 17개 화면과 주요 CRUD·체크·지도 흐름을 검증한다. 실제 외부 key가 없으면 `demo` 모드 검증을 완료하고 `live` 미검증 부분을 명확히 남긴다.
16. 안전하고 관련 있는 작업이 남아 있는 동안 중간 결과만 보고 멈추지 말고 완료까지 지속한다.
17. 완료 후 Issue를 갱신하고 PR을 만들며 CI와 변경 파일을 확인한 뒤 저장소 규칙에 따라 squash merge한다.

실제 AWS 배포는 이번 작업에서 하지 않는다. 대신 사용자가 완성본을 확인한 뒤 즉시 배포 작업을 시작할 수 있도록 필요한 Google·Kakao·S3·EC2·도메인·GitHub Secret 체크리스트를 실제 구현과 일치시켜라.

최종 보고에는 다음만 명확히 포함해줘.

- 로컬 실행 명령과 접속 주소
- 데모 로그인 방법
- 구현된 화면·기능
- 테스트와 브라우저 검증 결과
- 실제 Google·Kakao·S3를 쓰기 위해 사용자가 준비할 값
- 의도적으로 남긴 항목과 실제 AWS 배포에 필요한 다음 단계
- Issue·PR·병합 링크

---
