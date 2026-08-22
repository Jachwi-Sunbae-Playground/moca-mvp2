# 브랜치와 커밋

## 브랜치 전략

`main` 하나와 짧은 작업 브랜치만 유지한다.

| 브랜치 | 역할 |
| --- | --- |
| `main` | 항상 로컬 개발을 시작할 수 있는 기준선 |
| 작업 브랜치 | 한 이슈의 코드·설정·문서 변경을 함께 진행하고 PR로 병합하는 공간 |

- 작업 브랜치는 최신 `main`에서 분기하고 병합 후 삭제한다.
- `main`에 직접 커밋하거나 푸시하지 않는다.
- MVP2 개발 중 `main` 병합은 배포를 실행하지 않는다.
- 실제 배포 자동화는 [MVP2 배포 아키텍처](../operations/mvp2-deployment-architecture.md)의 준비 조건을 갖춘 뒤 수동으로만 실행한다.

## 브랜치 보호 기준

`main`에는 다음 최소 보호 규칙을 적용한다.

- PR을 거쳐 병합한다.
- 승인 인원은 요구하지 않는다. 대신 작성자가 변경 파일과 CI 결과를 직접 확인한다.
- 변경된 영역의 CI와 `Check docs consistency`가 성공해야 한다.
- 강제 푸시와 브랜치 삭제를 허용하지 않는다.

백엔드와 프론트엔드 CI는 관련 경로가 바뀐 PR에서만 실행한다. `main` push에는 다시 실행하지 않아 같은 커밋을 중복 검사하지 않는다.

## 브랜치 이름

```text
<type>/<issue-number>-<description>
codex/<description>
```

예시:

```text
feat/12-create-reservation
fix/24-prevent-duplicate-booking
refactor/31-separate-validation
codex/mvp2-transition-foundation
```

| type | 용도 |
| --- | --- |
| `feat` | 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 구조 개선 |
| `test` | 테스트 추가 |
| `docs` | 문서 수정 |
| `chore` | 설정 변경 |

## 커밋 메시지

커밋 메시지는 Conventional Commits를 사용하며, 타입은 위 표의 작업 성격에 맞게 선택한다.

```text
<type>: 변경 내용을 작성한다
```

예시:

```text
feat: 예약 생성 기능을 구현한다
fix: 중복 예약 문제를 수정한다
refactor: 검증 책임을 도메인으로 이동한다
test: 예약 예외 테스트를 추가한다
```

- 커밋 본문이 필요하면 한국어로 간결하게 작성한다.
- `수정`, `피드백 반영`처럼 목적이 드러나지 않는 메시지는 사용하지 않는다.
