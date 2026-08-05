# 백엔드 문서

백엔드 코드와 함께 변경되어야 하는 요구사항, 기술 결정, 실행 방법과 팀 합의를 `backend/docs`에서 버전 관리한다.

| 디렉터리 | 책임 |
| --- | --- |
| [`adr`](adr/README.md) | 기술 선택의 맥락, 대안, 트레이드오프와 재검토 조건 |
| [`requirements`](requirements/01-development-environment.md) | 지정·자율 요구사항의 수행 과정과 증거 |
| [`guides`](guides/local-development.md) | 개발자가 그대로 따라 할 수 있는 실행 절차 |
| [`conventions`](conventions/backend-code-convention.md) | 팀이 반복해서 적용하는 코드·API·테스트·Git 규칙 |
| [`architecture`](architecture/system-overview.md) | 시스템 경계, 패키지 구조와 데이터 모델 |
| [`api`](api/README.md) | 구현 전 API 합의와 구현 후 OpenAPI 연결 |
| [`operations`](operations/deployment.md) | 배포, 롤백, 모니터링과 장애 대응 절차 |

## 문서 작성 원칙

- 결정 이유와 대안은 ADR, 현재 지켜야 할 규칙은 컨벤션에 기록한다.
- 명령을 따라 하는 절차는 가이드, 수행 결과와 증거는 요구사항 문서에 기록한다.
- 아직 결정하지 않은 내용은 추측해 확정하지 않고 `미정` 또는 `초안`으로 표시한다.
- 코드나 설정이 바뀌면 같은 PR에서 관련 문서를 함께 수정한다.
- 파일명은 영문 `kebab-case`를 기본으로 사용한다.
