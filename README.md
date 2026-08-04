# 자취선배

자취를 준비하는 사용자가 주거지원 정보 확인부터 매물 확인, 계약, 입주 준비까지 놓치지 않도록 돕는 서비스입니다.

## 저장소 구조

이 저장소는 백엔드, 프론트엔드, 문서를 함께 관리하는 모노레포입니다.

```text
2026-jachwi-sunbae/
├── .github/       # CI 워크플로
├── backend/       # 백엔드 애플리케이션
├── config/        # 공통 개발 도구 설정
├── frontend/      # 프론트엔드 애플리케이션
├── docs/          # 요구사항, 가이드, ADR, 컨벤션
├── compose.yaml   # 로컬 공통 인프라
└── README.md
```

디렉터리는 실제 구성 요소나 문서가 추가되는 시점에 생성합니다.

## 시작하기

- [프로젝트 문서 안내](docs/README.md)
- [백엔드 로컬 개발 환경 구성 및 실행](docs/development/backend-local-setup.md)
