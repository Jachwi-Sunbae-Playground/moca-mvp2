# Moca MVP2

> 이 파일은 `CLAUDE.md`와 `AGENTS.md`에 같은 내용으로 유지한다. 한쪽을 고치면 다른 쪽도 같은 PR에서 고친다. 정합성 검사가 확인한다.

자취선배 MVP1을 기준으로 개인 계정에서 빠르게 실험하는 모노레포다. 백엔드는 Spring Boot, 프론트엔드는 React를 사용한다.

## 문서를 고치기 전에

- **시점 고정 문서는 고치지 않는다.** ADR, 실험 기록, 피벗 히스토리는 그때의 기록이다. 낡아 보여도 수정하지 말고 새 문서를 만든다. 각 문서 머리말의 `갱신 정책`을 확인한다.
- 같은 내용을 두 문서에 적지 않는다. 한 곳에 적고 나머지는 링크한다.
- `.md`를 수정했으면 `python3 .github/scripts/check_docs.py`를 실행한다.
- 규칙은 [문서 관리](docs/convention/documentation.md)를 따른다.

## 문서 경계

- `docs/` — 제품, 저장소 공통 규칙과 전체 운영 설계
- `backend/docs/` — 백엔드 코드와 함께 바뀌는 문서만 둔다.

## 작업 방식

- `main`에 직접 작업하거나 푸시하지 않는다. 짧은 작업 브랜치를 `main`에서 분기한다.
- 작업의 배경, 범위와 진행 상태는 GitHub Issue에 기록한다.
- 커밋은 `<type>: 변경 내용을 작성한다` 형식의 한국어 Conventional Commits.
- PR은 CI가 통과하고 스스로 변경 내용을 확인한 뒤 `Squash and merge`한다.
- 코드나 설정이 바뀌면 같은 PR에서 관련 문서를 함께 고친다.
- AWS 리소스 생성과 배포는 MVP2 기능 개발이 끝나고 명시적으로 요청받았을 때만 실행한다.
- 세부 규칙은 [컨벤션](docs/convention/README.md)과 [백엔드 문서](backend/docs/README.md)를 따른다.

## 문체

- `backend/docs/`, `docs/convention/`, `docs/operations/` — 평서형 `~한다`
- `docs/product/`, `README.md` — `~합니다`

## 백엔드

- 코드·API·예외 규칙은 [backend/docs/conventions](backend/docs/conventions/backend-code-convention.md)를 따른다.
