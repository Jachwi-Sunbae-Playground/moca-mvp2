# 자취선배

> 이 파일은 `CLAUDE.md`와 `AGENTS.md`에 같은 내용으로 유지한다. 한쪽을 고치면 다른 쪽도 같은 PR에서 고친다. 정합성 검사가 확인한다.

우테코 2026 자취선배. 백엔드(Spring Boot)와 프론트엔드를 함께 관리하는 모노레포.

## 문서를 고치기 전에

- **시점 고정 문서는 고치지 않는다.** ADR, 실험 기록, 피벗 히스토리는 그때의 기록이다. 낡아 보여도 수정하지 말고 새 문서를 만든다. 각 문서 머리말의 `갱신 정책`을 확인한다.
- 같은 내용을 두 문서에 적지 않는다. 한 곳에 적고 나머지는 링크한다.
- `.md`를 수정했으면 `python3 .github/scripts/check_docs.py`를 실행한다.
- 규칙은 [문서 관리](docs/convention/documentation.md)를 따른다.

## 문서 경계

- `docs/` — 제품·팀 공통 문서. 코드와 독립적으로 바뀐다.
- `backend/docs/` — 백엔드 코드와 함께 바뀌는 문서만 둔다.
- 새 문서의 위치가 애매하면 만들기 전에 묻는다.

## 작업 방식

- `main`과 `develop`에 직접 작업하거나 푸시하지 않는다. 작업 브랜치는 `develop`에서 분기한다.
- 커밋은 `<type>: 변경 내용을 작성한다` 형식의 한국어 Conventional Commits.
- PR 제목은 `[파트][작업 종류] 작업 내용`. 예: `[BE][Feat] 예약 생성 기능을 구현한다`
- 코드나 설정이 바뀌면 같은 PR에서 관련 문서를 함께 고친다.
- 세부 규칙은 [컨벤션](docs/convention/README.md)과 [백엔드 문서](backend/docs/README.md)를 따른다.

## 문체

- `backend/docs/`, `docs/convention/`, `docs/operations/` — 평서형 `~한다`
- `docs/product/`, `README.md` — `~합니다`

## 백엔드

- 코드·API·예외 규칙은 [backend/docs/conventions](backend/docs/conventions/backend-code-convention.md)를 따른다.
