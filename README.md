# 자취선배

자취를 준비하는 사용자가 주거지원 정보 확인부터 매물 확인, 계약, 입주 준비까지 놓치지 않도록 돕는 서비스입니다.

## 저장소 구조

이 저장소는 백엔드와 프론트엔드를 함께 관리하는 모노레포입니다. 백엔드 문서는 백엔드 코드와 같은 경계에서 관리합니다.

```text
2026-jachwi-sunbae/
├── .github/
│   └── workflows/        # CI 워크플로
├── backend/
│   ├── config/           # 백엔드 개발 도구 설정
│   ├── docs/             # 백엔드 문서
│   ├── gradle/           # Gradle Wrapper
│   ├── src/              # Spring Boot 소스와 테스트
│   ├── .env.example      # 백엔드 로컬 환경변수 예시
│   └── compose.yaml      # 백엔드 로컬 인프라
├── frontend/             # 프론트엔드 애플리케이션(개발 시작 시 생성)
├── .editorconfig         # 공통 에디터 설정
├── .gitignore            # Git 추적 제외 규칙
└── README.md
```

백엔드 문서 골격은 `backend/docs`에서 미리 관리하고, 프론트엔드처럼 아직 없는 애플리케이션 디렉터리는 실제 개발을 시작할 때 생성합니다.

## 시작하기

- [백엔드 문서 안내](backend/docs/README.md)
- [백엔드 로컬 개발 환경 구성 및 실행](backend/docs/guides/local-development.md)
