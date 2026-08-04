# 자취선배

자취를 준비하는 사용자가 주거지원 정보 확인부터 매물 확인, 계약, 입주 준비까지 놓치지 않도록 돕는 서비스입니다.

## 저장소 구조

이 저장소는 백엔드와 프론트엔드를 함께 관리하는 모노레포입니다. 백엔드 문서는 백엔드 코드와 같은 경계에서 관리합니다.

```text
2026-jachwi-sunbae/
├── .github/
│   └── workflows/        # CI 워크플로
├── backend/
│   ├── docs/             # 백엔드 요구사항, 실행 가이드, ADR, 컨벤션
│   ├── gradle/           # Gradle Wrapper
│   └── src/              # Spring Boot 소스와 테스트
├── config/
│   └── code-style/       # 공통 Java 코드 스타일
├── frontend/             # 프론트엔드 애플리케이션(개발 시작 시 생성)
├── .editorconfig         # 공통 에디터 설정
├── .env.example          # 로컬 환경변수 예시
├── .gitignore            # Git 추적 제외 규칙
├── compose.yaml          # 로컬 공통 인프라
└── README.md
```

디렉터리는 실제 구성 요소나 문서가 추가되는 시점에 생성합니다.

## 시작하기

- [백엔드 문서 안내](backend/docs/README.md)
- [백엔드 로컬 개발 환경 구성 및 실행](backend/docs/development/backend-local-setup.md)
