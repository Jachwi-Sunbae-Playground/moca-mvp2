![자취선배](docs/assets/key-visual.png)

# 자취선배

> **집은 매물 앱에서 찾고, 결정은 자취선배에서.**

여러 플랫폼에서 찾은 매물 후보를 한곳에 모으고, 방문 전·집보는 현장·계약 직전에 무엇을 확인할지 알려주며, 직접 확인한 상태와 메모를 매물별로 기록·비교하도록 돕는 **임차인 전용 집 선택 도구**입니다.

## 문서

제품의 방향과 팀 공통 규칙은 `docs/`, 백엔드 문서는 백엔드 코드와 같은 경계인 `backend/docs`에서 관리합니다.

### 제품

- [자취선배 개요](docs/product/overview.md) — 한 문장 소개, 제품 범위, 하지 않는 일, 장기 방향
- [문제와 사용자](docs/product/problem-and-users.md) — 해결하려는 문제, 타겟, 시장, 기존 플랫폼과의 차이
- [핵심 가설](docs/product/hypotheses.md) — 핵심·하위 가설과 현재 검증 상태
- [브랜드와 제품 원칙](docs/product/brand.md) — 미션, 비전, 제품 원칙, 브랜드 에셋
- [피벗 히스토리와 학습](docs/learnings/pivot-history.md) — 이전 검증에서 현재 방향까지의 학습
- [실험 기록](docs/experiments/) — 실험별 설계·결과·판정

### 팀 공통

- [컨벤션](docs/convention/README.md) — 브랜치·커밋, 이슈·PR, 코드 리뷰, 문서 관리
- [문서 관리](docs/convention/documentation.md) — 문서 분류, 정본과 대조 대상, 정합성 검사

### 운영

- [배포 아키텍처 설계](docs/operations/deployment-architecture.md) — 배포 대상, 플랫폼 선택과 근거, 비용 추정
- [CI/CD 배포 검증 기록](docs/operations/2026-08-20-cicd-deployment-validation.md) — 브랜치 보호, 리비전 검증과 자동 롤백의 실측 결과

## 저장소 구조

이 저장소는 백엔드와 프론트엔드를 함께 관리하는 모노레포입니다.

```text
2026-jachwi-sunbae/
├── .agents/
│   └── skills/           # Codex 검토 절차 (.claude/skills와 동일)
├── .claude/
│   ├── skills/           # Claude Code 검토 절차
│   └── settings.json     # 문서 정합성 훅
├── .codex/
│   └── hooks.json        # 문서 정합성 훅
├── .github/
│   ├── ISSUE_TEMPLATE/   # 이슈 템플릿
│   ├── scripts/          # 문서 정합성 검사와 훅 스크립트
│   ├── workflows/        # CI 워크플로
│   └── pull_request_template.md
├── docs/                 # 제품·팀 공통 문서 (코드와 독립)
│   ├── product/          # 개요, 문제·사용자, 가설, 브랜드
│   ├── convention/       # 브랜치·커밋, 이슈·PR, 코드 리뷰, 문서 관리
│   ├── experiments/      # 실험별 설계·결과·판정
│   ├── learnings/        # 피벗 히스토리와 학습
│   ├── operations/       # 배포 아키텍처 설계
│   └── assets/           # 브랜드 이미지
├── backend/
│   ├── config/           # 백엔드 개발 도구 설정
│   ├── docs/             # 백엔드 문서
│   ├── gradle/           # Gradle Wrapper
│   ├── src/              # Spring Boot 소스와 테스트
│   ├── .env.example      # 백엔드 로컬 환경변수 예시
│   └── compose.yaml      # 백엔드 로컬 인프라
├── frontend/             # 프론트엔드 애플리케이션
├── .editorconfig         # 공통 에디터 설정
├── .gitignore            # Git 추적 제외 규칙
├── AGENTS.md             # 에이전트가 매 세션 읽는 작업 규칙 (CLAUDE.md와 동일)
├── CLAUDE.md             # 에이전트가 매 세션 읽는 작업 규칙 (AGENTS.md와 동일)
└── README.md
```

이 구조는 [문서 관리](docs/convention/documentation.md)의 정합성 검사 대상이며, 디렉터리를 추가하면 이 트리도 같은 PR에서 수정합니다.

## 시작하기

- [백엔드 문서 안내](backend/docs/README.md)
- [백엔드 로컬 개발 환경 구성 및 실행](backend/docs/guides/local-development.md)
