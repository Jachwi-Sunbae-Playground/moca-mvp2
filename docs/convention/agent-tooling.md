# 에이전트 작업 환경

이 저장소는 Claude Code와 Codex를 함께 사용한다. 어느 쪽으로 작업하든 같은 규칙과 같은 검사가 적용되도록 설정을 대칭으로 유지한다.

## 대칭 구조

| 층 | Claude Code | Codex | 같게 유지하는 방법 |
| --- | --- | --- | --- |
| 규칙 파일 | `CLAUDE.md` | `AGENTS.md` | 내용이 같아야 한다. 검사 `A-3` |
| Skill | `.claude/skills/` | `.agents/skills/` | 파일 목록과 내용이 같아야 한다. 검사 `A-4` |
| 훅 | `.claude/settings.json` | `.codex/hooks.json` | 둘 다 [`docs_hook.sh`](../../.github/scripts/docs_hook.sh)를 호출해야 한다. 검사 `A-5` |
| CI 검사 | `.github/workflows/` | 같음 | 에이전트와 무관하다 |

한쪽만 고치면 [정합성 검사](documentation.md)가 실패한다. 어느 쪽이 정본인지 정하지 않는다. 두 파일은 대등하며 항상 같아야 한다.

**반드시 지켜야 하는 것은 CI에 둔다.** 훅과 Skill은 도구별로 갈리지만 CI는 사람이 편집기로 고치는 경우까지 포함해 모두 잡는다.

## 규칙 파일

`CLAUDE.md`와 `AGENTS.md`는 매 세션 컨텍스트에 들어간다. 읽지 않고 행동하면 틀리는 규칙만 적고 나머지는 링크한다. 컨벤션 내용을 옮겨 적지 않는다.

한쪽을 고치면 다른 쪽도 같은 PR에서 고친다.

## 훅

두 도구 모두 `PostToolUse` 이벤트에서 [`docs_hook.sh`](../../.github/scripts/docs_hook.sh)를 실행한다. 설정 형식은 다르지만 **판단 로직은 이 스크립트 한 곳에만 둔다.** 검사 대상을 바꿀 때는 스크립트만 고친다.

스크립트는 수정된 파일 경로를 알아내지 못하면 건너뛰지 않고 검사한다. 도구마다 이벤트 형식이 달라도 검사를 놓치지 않기 위해서다.

## Skill

Skill은 규칙이 아니라 **검토하는 방법과 순서**를 담는다. 컨벤션 문서의 내용을 옮겨 적으면 정합성 검사가 닿지 않는 사본이 하나 늘어난다.

두 도구가 같은 `SKILL.md` 형식을 사용하므로 파일을 그대로 복사해 미러로 유지한다.

### 현재 Skill

| Skill | 용도 |
| --- | --- |
| `docs-review` | 기계 검사가 잡지 못하는 문서 모순·중복·결정 누락을 검토한다 |

### 추가 예정 Skill

대상 코드가 없는 동안에는 만들지 않는다. 쓸 일이 없는 Skill은 판단만 흐린다. 아래 조건에 도달하면 만들고, 만들 때 두 디렉터리에 함께 넣는다.

| Skill | 만드는 시점 | 담을 내용 |
| --- | --- | --- |
| `api-design` | **첫 Controller를 구현할 때** | 계약 합의 → 프론트엔드 검토 → 구현 → Swagger 동기화 절차와, [API 컨벤션](../../backend/docs/conventions/api-convention.md)·[예외 컨벤션](../../backend/docs/conventions/exception-convention.md)을 오가는 순서 |
| `new-domain` | **첫 도메인 패키지를 만들 때** | [패키지 구조](../../backend/docs/architecture/backend-package-structure.md)·레이어 책임·DTO·예외를 한 번에 훑는 순서 |

### 만들지 않는 Skill

이미 문서와 검사가 담당하므로 만들지 않는다.

| 후보 | 대신 담당하는 것 |
| --- | --- |
| ADR 작성 | [ADR 작성 규칙](../../backend/docs/adr/README.md)과 목차 검사 `B-1` |
| PR 준비 | `.github/pull_request_template.md`와 규칙 파일 |
| 컨벤션 변경 | `docs-review`의 결정 누락 검토 |
