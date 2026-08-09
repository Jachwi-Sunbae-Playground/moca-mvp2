# Claude Code 작업 환경

이 저장소에서 Claude Code가 따르는 규칙과 자동 장치를 정리한다.

## 장치와 역할

| 장치 | 위치 | 실행 주체 | 보장 |
| --- | --- | --- | --- |
| 프로젝트 규칙 | `CLAUDE.md` | 매 세션 자동 주입 | 항상 읽힌다 |
| 문서 정합성 훅 | `.claude/settings.json` | Claude Code 하버스 | 문서 수정 시 항상 실행된다 |
| Skill | `.claude/skills/` | 모델 판단 | 관련 있다고 판단할 때만 열린다 |
| CI 검사 | `.github/workflows/` | GitHub Actions | PR마다 항상 실행된다 |

반드시 지켜야 하는 것은 훅과 CI에 둔다. 사람의 판단이 필요한 검토는 Skill에 둔다. Skill은 모델이 호출 여부를 판단하므로 확정적인 장치로 쓰지 않는다.

## CLAUDE.md

매 세션 컨텍스트에 들어가므로 읽지 않고 행동하면 틀리는 규칙만 적고 나머지는 링크한다. 컨벤션 내용을 옮겨 적지 않는다.

## 문서 정합성 훅

`.claude/settings.json`의 `PostToolUse` 훅이 `.md`, `.env.example`, 이슈·PR 템플릿을 수정한 직후 [정합성 검사](documentation.md)를 실행한다. 실패하면 결과가 즉시 피드백되어 CI까지 가기 전에 고칠 수 있다.

훅은 Claude Code로 작업할 때만 동작한다. 편집기로 직접 고치는 경우를 위해 CI 검사를 함께 둔다.

## Skill

Skill은 규칙이 아니라 **검토하는 방법과 순서**를 담는다. 컨벤션 문서의 내용을 옮겨 적으면 정합성 검사가 닿지 않는 사본이 하나 늘어난다.

### 현재 Skill

| Skill | 용도 |
| --- | --- |
| `docs-review` | 기계 검사가 잡지 못하는 문서 모순·중복·결정 누락을 검토한다 |

### 추가 예정 Skill

대상 코드가 없는 동안에는 만들지 않는다. 쓸 일이 없는 Skill은 판단만 흐린다. 아래 조건에 도달하면 만든다.

| Skill | 만드는 시점 | 담을 내용 |
| --- | --- | --- |
| `test-review` | **첫 도메인 테스트를 작성할 때** | [테스트 전략](../../backend/docs/conventions/test-strategy.md)의 6가지 테스트 종류 중 무엇으로 검증할지 정하는 판단 순서. 특히 Service 단위 테스트의 사용·미사용 조건 |
| `api-design` | **첫 Controller를 구현할 때** | 명세 작성 → 프론트엔드 검토 → 구현 → Swagger 동기화 절차와, [API 컨벤션](../../backend/docs/conventions/api-convention.md)·[예외 컨벤션](../../backend/docs/conventions/exception-convention.md)을 오가는 순서 |
| `new-domain` | **첫 도메인 패키지를 만들 때** | [패키지 구조](../../backend/docs/architecture/backend-package-structure.md)·레이어 책임·DTO·예외·테스트를 한 번에 훑는 순서 |

### 만들지 않는 Skill

이미 문서와 검사가 담당하므로 만들지 않는다.

| 후보 | 대신 담당하는 것 |
| --- | --- |
| ADR 작성 | [ADR 작성 규칙](../../backend/docs/adr/README.md)과 목차 검사 `B-1` |
| PR 준비 | `.github/pull_request_template.md`와 `CLAUDE.md` |
| 컨벤션 변경 | `docs-review`의 결정 누락 검토 |
