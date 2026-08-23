![자취선배](docs/assets/key-visual.png)

# Moca MVP2

> **집은 매물 앱에서 찾고, 결정은 자취선배에서.**

여러 플랫폼에서 찾은 매물 후보를 한곳에 모으고, 방문 전·집보는 현장·계약 직전에 무엇을 확인할지 알려주며, 직접 확인한 상태와 메모를 매물별로 기록·비교하도록 돕는 **임차인 전용 집 선택 도구**입니다.

이 저장소는 [자취선배 원본 저장소](https://github.com/woowacourse-teams/2026-jachwi-sunbae)의 MVP1 `develop`을 전체 Git 이력과 함께 옮긴 개인 MVP2 실험 공간입니다. 기준 커밋은 `021a1b8b2565323a8963f6e8fe6dc72bb33eff0b`, 태그는 `mvp1-baseline`입니다.

현재는 로컬에서만 개발합니다. 개인 AWS용 최소 배포 파일은 준비했지만 AWS 리소스를 생성하거나 실제 배포하지 않았습니다.

## 문서

제품의 방향과 저장소 공통 규칙은 `docs/`, 백엔드 문서는 백엔드 코드와 같은 경계인 `backend/docs`에서 관리합니다.

### 제품

- [제품 문서 안내](docs/product/README.md) — MVP1 기준선부터 MVP2 구현 문서까지의 읽기 순서
- [자취선배 개요](docs/product/overview.md) — 한 문장 소개, 제품 범위, 하지 않는 일, 장기 방향
- [문제와 사용자](docs/product/problem-and-users.md) — 해결하려는 문제, 타겟, 시장, 기존 플랫폼과의 차이
- [핵심 가설](docs/product/hypotheses.md) — 핵심·하위 가설과 현재 검증 상태
- [브랜드와 제품 원칙](docs/product/brand.md) — 미션, 비전, 제품 원칙, 브랜드 에셋
- [MVP1 실제 구현 기준선](docs/product/scope/mvp1-baseline.md) — 태그 시점의 기능·한계와 전달 문서 차이
- [MVP2 범위](docs/product/scope/mvp2-scope.md) — 유지·변경·신규·제외 범위와 완료 기준
- [MVP2 구현 브리프](docs/product/mvp2-implementation-brief.md) — 한 번에 구현할 때의 정본과 작업 순서
- [MVP2 단일 구현 프롬프트](docs/product/mvp2-implementation-prompt.md) — 다음 Codex 작업에 그대로 전달할 실행 지시문
- [피벗 히스토리와 학습](docs/learnings/pivot-history.md) — 이전 검증에서 현재 방향까지의 학습
- [실험 기록](docs/experiments/) — 실험별 설계·결과·판정

### 저장소 공통

- [컨벤션](docs/convention/README.md) — 브랜치·커밋, 이슈·PR, 코드 리뷰, 문서 관리
- [문서 관리](docs/convention/documentation.md) — 문서 분류, 정본과 대조 대상, 정합성 검사

### 운영

- [MVP2 전환 기준](docs/operations/mvp2-transition.md) — Git 기준선, 제거·유지·대체 항목과 작업 추적
- [MVP2 배포 아키텍처](docs/operations/mvp2-deployment-architecture.md) — 향후 단일 EC2 최소 구성과 준비 조건
- [MVP1 배포 아키텍처 기록](docs/operations/deployment-architecture.md) — 기존 우테코 AWS 구성
- [MVP1 CI/CD 배포 검증 기록](docs/operations/2026-08-20-cicd-deployment-validation.md) — 기존 리비전 검증과 자동 롤백의 실측 결과

### 디자인

- [MVP2 와이어프레임](docs/design/wireframes/README.md) — `.pen` 원본, 17개 화면 PNG와 화면 연결 기준

## 저장소 구조

이 저장소는 백엔드와 프론트엔드를 함께 관리하는 모노레포입니다.

```text
moca-mvp2/
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
├── deploy/               # 향후 단일 EC2 최소 배포 파일 (현재 미배포)
├── docs/                 # 제품·저장소 공통 문서
│   ├── product/          # MVP 기준선·범위·기능 명세·결정·흐름
│   ├── design/           # MVP2 와이어프레임 원본과 화면별 PNG
│   ├── convention/       # 브랜치·커밋, 이슈·PR, 코드 리뷰, 문서 관리
│   ├── experiments/      # 실험별 설계·결과·판정
│   ├── learnings/        # 피벗 히스토리와 학습
│   ├── operations/       # MVP2 전환·배포 설계와 MVP1 기록
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

외부 키 없이 MVP2를 실행하려면 `backend/.env.example`과 `frontend/.env.example`을 각각 복사한 뒤 MySQL·MinIO, 백엔드, 프론트엔드 순으로 실행합니다. 접속 주소는 `http://localhost:3000`이고 로그인 화면의 `데모로 시작하기`를 사용합니다.
