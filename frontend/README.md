# 자취기록 프론트엔드

Webpack으로 직접 구성한 React + TypeScript 프로젝트입니다.

## 기술 스택

- React 19
- TypeScript 6
- Webpack 5
- ESLint
- Prettier

## 설치

```bash
npm install
```

## 실행

```bash
npm run dev
```

개발 서버는 `http://localhost:3000`에서 실행됩니다.

## 주요 명령어

```bash
npm run typecheck     # TypeScript 타입 검사
npm run lint          # ESLint 검사
npm run lint:fix      # ESLint 자동 수정
npm run format        # Prettier 포맷 적용
npm run format:check  # Prettier 포맷 검사
npm run build         # 프로덕션 빌드
```

자세한 코드 작성 규칙은 [프론트엔드 컨벤션](./docs/FRONTEND_CONVENTIONS.md)을 참고합니다.

## 디렉터리 구조

```text
src/
├── assets/       # Webpack으로 처리할 이미지·정적 자원
├── App.tsx       # 애플리케이션 컴포넌트
├── main.tsx      # Webpack 진입점
├── style.css     # 전역 스타일
└── global.d.ts   # 정적 자원 타입 선언
```
