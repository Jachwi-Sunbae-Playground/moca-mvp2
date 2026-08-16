# 프론트엔드 컨벤션

현재 프로젝트에서 코드를 읽고 작성하는 기준입니다. 규칙은 목적 없이 늘리지 않고, 작업 중 반복해서 문제가 생기는 지점부터 추가합니다.

## 1. 디렉터리 구조

```text
frontend/
├── public/                 # URL로 직접 제공할 정적 파일
├── src/
│   ├── apis/               # API 요청과 응답 처리
│   │   └── dtos/           # 네트워크 통신용 DTO
│   ├── app/                # 앱 전체 레이아웃과 라우팅
│   ├── assets/             # import하여 Webpack이 처리할 이미지·폰트
│   ├── components/         # 재사용 컴포넌트
│   ├── constants/          # 변하지 않는 값
│   ├── hooks/              # 상태와 비즈니스 로직
│   │   └── query/          # 조회·변경 요청을 조합한 Hook
│   ├── types/              # 공통 TypeScript 타입
│   └── utils/              # 공통 순수 함수
├── webpack.config.js
└── tsconfig.json
```

`public`과 `src/assets`는 구분합니다.

- `public`: 코드에서 import하지 않고 `/logo.svg`처럼 URL로 직접 참조하는 파일
- `src/assets`: `import logo from './assets/logo.svg'`처럼 가져와 Webpack이 처리하는 파일

기능에만 사용하는 파일은 가능한 한 해당 기능 폴더 가까이에 둡니다. 여러 기능에서 공유될 때만 `components`, `hooks`, `utils` 등 공용 영역으로 이동합니다.

## 2. 네이밍

### 파일과 폴더

- 컴포넌트 파일: `PascalCase` — `PostCard.tsx`
- 함수·변수 파일: `camelCase` — `formatDate.ts`
- 폴더: `kebab-case` — `post-detail/`
- Hook 파일: `usePascalCase` — `usePost.ts`
- 타입: `PascalCase` — `Post`, `PostDetailProps`
- Boolean: `is`, `has`, `can`, `should` 접두사 — `isLoading`, `hasNextPage`
- 상수: 의미가 분명하면 `camelCase`, 전역 불변 값은 `UPPER_SNAKE_CASE`

### API 함수

- GET: `fetch` — `fetchPostDetail.ts`
- POST: `submit` 또는 `create` — `submitPost.ts`
- DELETE: `remove` — `removePost.ts`
- PUT/PATCH: `update` — `updatePostDetail.ts`

API 함수는 컴포넌트에서 직접 호출하지 않고 API 모듈이나 Query Hook을 통해 사용합니다.

## 3. 모듈과 export

- 컴포넌트: `default export`
- 일반 함수·상수·타입: `named export`
- 타입만 가져올 때는 `import type` 사용

```tsx
import type { Post } from '../types/Post';
import { formatDate } from '../utils/formatDate';
```

불필요한 `index.ts` 재-export와 순환 의존성은 만들지 않습니다.

## 4. React 컴포넌트

- 컴포넌트는 화살표 함수로 선언합니다.
- Props 타입을 선언하고 구조분해 할당합니다.
- 컴포넌트는 화면 표현에 집중하고, API 호출과 복잡한 상태 로직은 Hook으로 분리합니다.
- 하나의 컴포넌트가 너무 커지면 화면·도메인·표현 책임을 나눕니다.

```tsx
type PostCardProps = {
  title: string;
  onClick: () => void;
};

const PostCard = ({ title, onClick }: PostCardProps) => {
  return (
    <button type="button" onClick={onClick}>
      {title}
    </button>
  );
};

export default PostCard;
```

## 5. TypeScript

- `any`는 사용하지 않고, 외부에서 알 수 없는 값은 `unknown`으로 받습니다.
- API 응답은 DTO 타입으로 받고, 화면에서 사용하는 타입과 필요하면 변환합니다.
- 타입 단언(`as`)과 non-null assertion(`!`)은 근거가 있을 때만 사용합니다.
- 공통 타입은 `src/types`, 특정 기능에서만 사용하는 타입은 해당 기능 폴더에 둡니다.
- 타입 검사와 Babel 변환을 구분합니다. Babel은 타입을 검사하지 않으므로 `npm run typecheck`를 실행합니다.

## 6. CSS

- 새로 작성하거나 수정하는 화면·컴포넌트의 종속 스타일은 같은 위치의 `*.module.css`에 두고 CSS Modules로 가져옵니다.
- CSS Modules의 클래스 이름은 TypeScript에서 바로 읽을 수 있도록 `camelCase`를 사용합니다.
- 디자인 토큰은 `src/styles/tokens.css`, 요소 기본값은 `src/styles/global.css`, 여러 화면에서 재사용하는 유틸리티 클래스는 `src/styles/utilities.css`에 둡니다.
- 전역 클래스는 실제로 여러 화면에서 같은 의미와 형태로 재사용할 때만 추가합니다.
- 스타일 목적으로 ID 선택자를 사용하지 않습니다.
- 컴포넌트 스타일은 해당 컴포넌트와 가까운 위치에 둡니다.
- `rem`: 폰트 크기와 주요 간격
- `px`: 테두리처럼 고정되어야 하는 얇은 선
- `%`, `vw`, `vh`: 부모나 화면 크기에 반응해야 하는 영역
- 색상·간격·radius처럼 반복되는 값은 CSS 변수로 관리합니다.

```css
:root {
  --color-primary: #2563eb;
  --space-md: 1rem;
  --radius-md: 0.5rem;
}
```

```tsx
import styles from './PostCard.module.css';

const PostCard = () => <article className={styles.card}>...</article>;
```

## 7. API와 상태 처리

- API 통신 코드는 `src/apis`에 둡니다.
- 서버 응답 형식과 화면 모델이 다르면 API 경계에서 변환합니다.
- 조회·변경 요청의 로직은 `src/hooks/query`에 둡니다.
- 화면에는 최소한 loading, error, empty, success 상태를 고려합니다.
- 서버 상태와 UI 상태를 구분합니다. 서버에서 가져온 데이터를 불필요하게 여러 컴포넌트의 로컬 상태로 복사하지 않습니다.

## 8. 접근성

- 이미지에는 의미에 맞는 `alt`를 작성합니다.
- 클릭 동작에는 가능한 한 `div` 대신 `button`이나 `a`를 사용합니다.
- 폼 입력에는 label을 제공합니다.
- 키보드만으로도 주요 기능을 사용할 수 있어야 합니다.
- 색상만으로 상태를 전달하지 않습니다.

## 9. 검사 명령어

```bash
npm run typecheck
npm run lint
npm run format:check
npm run test
npm run build
```

코드를 제출하기 전에 타입 검사, 린트, 포맷 검사, 테스트, 빌드를 모두 통과시키는 것을 기준으로 합니다.
