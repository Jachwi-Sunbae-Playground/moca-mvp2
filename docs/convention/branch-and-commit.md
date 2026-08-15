# 브랜치와 커밋

## 브랜치 전략

`main`과 `develop` 두 브랜치를 유지하고, 작업은 짧은 작업 브랜치에서 한다.

| 브랜치 | 역할 | 배포 대상 |
| --- | --- | --- |
| `main` | 운영에 배포되는 가장 안정적인 코드를 관리한다 | prod |
| `develop` | 기본 브랜치. 작업 브랜치가 모이는 통합 지점이다 | dev |
| 작업 브랜치 | 기능 추가·수정 단위로 `develop`에서 분기해 PR로 되돌린다. 병합 후 삭제한다 | — |

- 작업은 짧은 작업 브랜치에서 진행한다.
- `main`과 `develop`에 직접 작업하거나 푸시하지 않는다.
- **두 브랜치 모두 병합이 배포 트리거다.** `develop` 병합은 dev에, `main` 병합은 prod에 배포된다([배포 아키텍처 설계](../operations/deployment-architecture.md)).
- `main`으로는 `develop`에서 **PR로 승격**한다. 작업 브랜치를 `main`에 직접 병합하지 않는다.

```text
작업 브랜치 ─PR→ develop ─PR→ main
                  ↓            ↓
                 dev          prod
```

운영으로 나가는 것을 diff로 확인하기 위해 승격도 PR로 한다. dev에서 실제 인프라로 확인한 뒤 올린다.

## 브랜치 이름

```text
<type>/<issue-number>-<description>
```

예시:

```text
feat/12-create-reservation
fix/24-prevent-duplicate-booking
refactor/31-separate-validation
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

커밋 메시지는 Conventional Commits를 사용하며, 타입은 브랜치 이름과 같은 값을 쓴다.

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
