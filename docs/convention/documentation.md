# 문서 관리

문서가 잘 정리되어 있어도 갱신되지 않으면 오히려 잘못된 판단을 만든다. 이 문서는 어떤 문서를 무엇과 맞춰야 하는지, 무엇을 기계가 검사하는지 정한다.

## 문서 분류

문서를 고쳐야 할지 판단할 때 다음을 묻는다.

> **이 문서가 틀렸을 때, 무엇을 보고 고쳐야 하는가?**

| 분류 | 정본 | 틀렸을 때 |
| --- | --- | --- |
| 파생 | 코드·설정·다른 문서 | 정본을 보고 문서를 고친다 |
| 규범 | 문서 자체 | 코드를 고친다. 문서가 맞다 |
| 시점 고정 | 그때의 판단 | **고치지 않는다.** 고치면 기록의 가치가 사라진다 |

모든 문서를 이 셋으로 정확히 나누지는 않는다. 지금은 **확실한 시점 고정**과 **확실한 파생**만 문서 머리말에 표시하고, 나머지는 표시하지 않는다.

아직 코드가 없어 계획에 가까운 문서는 파생으로 둔다. 예를 들어 [백엔드 패키지 구조](../../backend/docs/architecture/backend-package-structure.md)는 실제 코드가 생기면 규범으로 옮긴다.

## 머리말 표기

기존 문서의 `- 상태: 초안` 관례를 확장해 제목 바로 아래에 적는다.

파생 문서는 무엇과 맞춰야 하는지 적는다.

```markdown
- 문서 성격: 파생
- 대조 대상: `backend/.env.example`
```

시점 고정 문서는 갱신하지 않는다는 사실을 적는다.

```markdown
- 문서 성격: 시점 고정
- 갱신 정책: 그 시점의 기록이므로 갱신하지 않는다
```

## 시점 고정 문서

다음 문서는 낡아 보여도 고치지 않는다. 내용을 바꿔야 한다면 새 문서를 만든다.

| 문서 | 이유 |
| --- | --- |
| [ADR](../../backend/docs/adr/README.md) `0001`~`0008` | 그때의 결정 맥락을 보존한다 |
| [실험 기록](../experiments/) | 실험 설계와 결과의 기록이다 |
| [피벗 히스토리](../learnings/pivot-history.md) | 이전 검증에서 현재까지의 학습 기록이다 |

### ADR은 부분 동결이다

ADR은 전체를 동결하지 않는다. [ADR 작성 규칙](../../backend/docs/adr/README.md)에 따라 다음과 같이 나눈다.

- **고정**: `맥락`, `결정`, `근거`, `검토한 대안`
- **갱신**: `결과와 트레이드오프`, `재검토 조건`

결론을 바꿀 때는 기존 ADR을 수정하지 않고 새 ADR에서 대체한다.

## 파생 문서와 대조 대상

| 문서 | 대조 대상 | 검사 |
| --- | --- | --- |
| [`.github/ISSUE_TEMPLATE/issue.md`](../../.github/ISSUE_TEMPLATE/issue.md) | [이슈와 PR](issue-and-pr.md)의 이슈 본문 | A-2 |
| [`.github/pull_request_template.md`](../../.github/pull_request_template.md) | [이슈와 PR](issue-and-pr.md)의 PR 본문 | A-2 |
| [`docs/convention/README.md`](README.md) | `docs/convention/*.md` 파일 목록 | B-1 |
| [`backend/docs/adr/README.md`](../../backend/docs/adr/README.md) | `adr/NNNN-*.md` 파일 목록 | B-1 |
| [`backend/docs/README.md`](../../backend/docs/README.md) | `backend/docs/*` 디렉터리 목록 | B-1 |
| [`README.md`](../../README.md) | 최상위 디렉터리와 `docs/` 하위 디렉터리 | B-1 |
| [환경변수](../../backend/docs/guides/environment-variables.md) | `backend/.env.example` | B-2 |
| [로컬 개발](../../backend/docs/guides/local-development.md) | `compose.yaml`, `build.gradle`, 실행 URL | 사람 |
| [백엔드 패키지 구조](../../backend/docs/architecture/backend-package-structure.md) | `build.gradle`의 `group`, `backend/src` 구조 | 사람 |
| [시스템 개요](../../backend/docs/architecture/system-overview.md) | 실제 구성 요소 | 사람 |
| [배포](../../backend/docs/operations/deployment.md) | `backend/deploy/`, 실제 AWS 파이프라인 구성 | 사람 |
| [롤백](../../backend/docs/operations/rollback.md) | 실제 CodeDeploy 배포 그룹 설정 | 사람 |
| [배포 아키텍처 설계](../operations/deployment-architecture.md) | 실제 AWS 리소스 구성 | 사람 |
| [핵심 가설](../product/hypotheses.md) | [실험 기록](../experiments/)의 결과 | 사람 |
| [`frontend/README.md`](../../frontend/README.md) | `frontend/` 구조와 `package.json` | 사람 |
| [프론트엔드 배포](../../frontend/docs/deployment.md) | `frontend/webpack.config.js`, 실제 CloudFront·파이프라인 구성 | 사람 |
| [`CLAUDE.md`](../../CLAUDE.md) | `docs/convention/`과 `backend/docs/conventions/`의 규칙 | 사람 |
| [`AGENTS.md`](../../AGENTS.md) | `CLAUDE.md` | A-3 |
| `.agents/skills/` | `.claude/skills/` | A-4 |

`검사` 열이 `사람`인 문서는 아직 대조 대상이 없거나 서술형이라 기계가 판단하기 어렵다. 코드가 생기면 검사로 옮긴다.

## 정합성 검사

```bash
python3 .github/scripts/check_docs.py
```

모든 PR에서 자동으로 실행되며, 실패하면 병합할 수 없다. 검사가 몇 초로 끝나므로 경로 필터를 두지 않는다. 검사 대상이 늘었을 때 필터를 빠뜨리는 편이 더 위험하다.

Claude Code나 Codex로 작업할 때는 문서를 수정한 직후 같은 스크립트가 훅으로 실행된다. 설정과 Skill은 [에이전트 작업 환경](agent-tooling.md)에 정리한다.

| 검사 | 내용 |
| --- | --- |
| A-1 | 모든 `.md`의 상대 링크가 실제 파일을 가리킨다 |
| A-2 | 이슈·PR 템플릿이 [이슈와 PR](issue-and-pr.md)의 본문과 정확히 같다 |
| A-3 | `CLAUDE.md`와 `AGENTS.md`의 내용이 같다 |
| A-4 | Claude Code와 Codex의 Skill이 같다 |
| A-5 | 두 에이전트의 훅이 공용 스크립트를 사용한다 |
| B-1 | 목차 문서가 실제 파일·디렉터리 목록과 같다 |
| B-2 | 환경변수 표가 `backend/.env.example`과 같다 |

검사를 추가할 때는 이 표와 위의 대조 대상 표를 함께 수정한다.

### 아직 검사하지 않는 것

다음은 대조 대상이 아직 존재하지 않아 검사에 넣지 않았다. 해당 시점이 되면 추가한다.

| 검사 | 추가 시점 |
| --- | --- |
| 패키지 트리와 실제 구조 대조 | 첫 도메인 패키지를 만들 때 |

## 작성 원칙

- 코드나 설정이 바뀌면 같은 PR에서 관련 문서를 함께 수정한다.
- 같은 내용을 두 문서에 적지 않는다. 한 곳에 적고 나머지는 링크한다.
- 관리하지 않을 문서는 만들지 않는다. 문서가 늘어난 만큼 어긋날 수 있는 자리도 늘어난다.
- 아직 결정하지 않은 내용은 추측해 확정하지 않고 `미정` 또는 `초안`으로 표시한다.
- 파일명은 영문 `kebab-case`를 기본으로 사용한다.
