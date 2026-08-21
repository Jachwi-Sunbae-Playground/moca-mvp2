# 자취선배 프론트엔드

- 문서 성격: 파생
- 대조 대상: `frontend/package.json`, `frontend/webpack.config.js`, `frontend/.env.example`, `frontend/src/`, `.github/workflows/frontend-ci.yml`

Webpack으로 구성한 React + TypeScript SPA입니다. 현재 코드는 초기 백엔드 프로토타입 계약을 기준으로 작성되어 있으며, 새 백엔드 구현과 연결할 때 Swagger/OpenAPI를 기준으로 갱신합니다.

## 백엔드 API 계약

백엔드 API 계약은 별도의 Markdown 명세로 복제하지 않고 실행 중인 백엔드의 Swagger UI와 `/v3/api-docs`에서 확인합니다. 프론트엔드의 DTO, 런타임 파서와 API 함수는 구현된 OpenAPI 계약에 맞춰 함께 갱신합니다.

## 기술 스택

- React 19, TypeScript 6 strict, Webpack 5
- React Router: SPA 라우팅과 보호 경로
- TanStack Query: 현재 회원·매물·체크리스트·방문처럼 서버에서 조회하는 상태
- Vitest, React Testing Library, MSW: 사용자 흐름과 HTTP 경계 테스트
- ESLint, Prettier

React Router와 TanStack Query는 라우팅·서버 상태라는 서로 다른 책임만 담당합니다. 미래 화면을 예상한 전역 상태 관리 라이브러리와 UI 프레임워크는 사용하지 않습니다.

## 설치와 실행

Node.js 버전은 [`.nvmrc`](./.nvmrc)의 `22.23.1`로 고정합니다.

```bash
nvm use
npm ci
```

asdf를 사용한다면 `frontend/`에서 같은 버전을 설치하고 실행합니다. IDE나 자동화의 비대화형 셸에서 `npm: command not found`가 발생하면 전역 Node.js를 다시 설치하거나 다른 패키지 매니저로 바꾸지 않고 `asdf exec`로 프로젝트 버전을 직접 실행합니다.

```bash
asdf install nodejs 22.23.1
asdf current nodejs
asdf exec npm ci
asdf exec npm run dev
```

`asdf current nodejs`가 `22.23.1`이 아니면 셸 초기화보다 먼저 현재 작업 디렉터리가 `frontend/`인지와 asdf의 legacy version file 지원 설정을 확인합니다. 이후 검사 명령도 `asdf exec npm run typecheck`처럼 같은 방식으로 실행할 수 있습니다.

[`.env.example`](./.env.example)을 복사하고 공개 환경값을 로컬 값으로 변경합니다. Webpack은 `.env` 파일을 자동으로 읽지 않으므로 실행할 셸에 값을 내보내야 합니다.

```bash
cp .env.example .env.local
set -a
source .env.local
set +a
npm run dev
```

개발 서버는 `http://localhost:3000`에서 실행됩니다. 필수 환경변수가 없거나 URL 형식이 잘못되면 애플리케이션이 설정 오류 화면을 표시합니다.

실제 백엔드나 Google OAuth 없이 화면 흐름을 확인할 때는 로컬 전용 MSW 모킹 환경을 사용합니다.

```bash
npm run dev:mock
```

`dev:mock`은 개발 빌드에서만 `ENABLE_MSW`를 활성화하고 브라우저 요청을 로컬 fixture로 처리합니다. 프로덕션 빌드는 이 값과 관계없이 MSW를 비활성화하므로 dev·prod 배포의 실제 API 오류를 가리지 않습니다.

## 공개 환경변수

| 환경변수              | 로컬 예시                                      | 용도                                  |
| --------------------- | ---------------------------------------------- | ------------------------------------- |
| `API_BASE_URL`        | `http://localhost:8080`                        | 백엔드 API 기준 URL                   |
| `GOOGLE_CLIENT_ID`    | `example-client-id.apps.googleusercontent.com` | 공개 Google Web OAuth Client ID       |
| `GOOGLE_REDIRECT_URI` | `http://localhost:3000/oauth/google/callback`  | Google과 백엔드가 허용한 callback URI |

`GOOGLE_CLIENT_ID`는 공개 설정값이지만 운영 값을 예시 파일에 커밋하지 않습니다. Google Client Secret, JWT 비밀키와 Access Token은 프론트엔드 환경변수에 두지 않습니다.

## Google OAuth 로컬 설정

실제 Google 로그인을 확인할 때 다음 세 위치의 값을 정확히 맞춥니다.

1. Google Cloud Console의 Web OAuth Client에 `http://localhost:3000/oauth/google/callback`을 승인된 redirect URI로 등록합니다.
2. 프론트엔드 `GOOGLE_CLIENT_ID`와 백엔드 `GOOGLE_OAUTH_CLIENT_ID`에 같은 Web Client ID를 사용합니다.
3. 프론트엔드 `GOOGLE_REDIRECT_URI`를 백엔드 `GOOGLE_OAUTH_ALLOWED_REDIRECT_URIS`에 정확히 등록하고, 백엔드 `CORS_ALLOWED_ORIGINS`에 `http://localhost:3000`을 둡니다.

Client Secret은 백엔드에만 설정합니다. 백엔드 실행 방법과 비밀값 관리는 [백엔드 환경변수 가이드](../backend/docs/guides/environment-variables.md)를 따릅니다.

로그인 버튼은 Google Authorization Endpoint로 이동하기 전에 PKCE S256 `codeVerifier`·`codeChallenge`, `state`, `nonce`를 생성합니다. callback의 `state`가 일치할 때만 백엔드 인증 API를 호출합니다. 구체적인 계약은 Swagger/OpenAPI에서 확인합니다.

| 경로                     | 기능                                                                |
| ------------------------ | ------------------------------------------------------------------- |
| `/login`                 | Google 로그인 시작, 탭 단위 인증 유지 정책 안내                     |
| `/oauth/google/callback` | PKCE 일회성 값 소비·URL query 제거·API-001 교환·API-002 회원 확인   |
| `/`                      | 인증 후 `/properties`, 비인증이면 `/login`으로 이동                 |
| 알 수 없는 경로          | 인증 여부에 맞는 시작 화면 링크를 제공하는 안전한 404 fallback 표시 |

## 인증 저장 정책

Access Token, `tokenType`, `expiresAt`은 같은 탭의 새로고침에서 인증을 복원할 수 있도록 `sessionStorage`에 저장합니다. 애플리케이션은 렌더링 전에 저장값의 형식과 만료 시각을 검증한 뒤 메모리 인증 상태와 만료 타이머를 복원합니다. 현재 회원·매물·사진·체크리스트·방문 Query Cache는 메모리에만 두며 JWT payload를 인증 상태나 회원 정보의 정본으로 사용하지 않습니다.

인증은 탭 단위로 관리되어 같은 탭의 새로고침에서는 유지되고 탭 종료와 브라우저 재실행 시 삭제됩니다. `localStorage`, IndexedDB와 JavaScript에서 읽는 영속 쿠키에는 Access Token을 저장하지 않습니다. 401, 토큰 만료와 로그아웃은 메모리·`sessionStorage` 인증 정보와 인증 관련 Query Cache를 함께 정리합니다. 일시적인 네트워크 실패는 인증 만료로 처리하지 않습니다.

Google 리다이렉트 왕복에 필요한 `codeVerifier`, `state`, `nonce`도 인증 세션과 구분된 키로 `sessionStorage`에 일회성 보관합니다. callback 성공·실패·취소·state 불일치·code 누락을 포함한 모든 종료 경로에서 읽는 즉시 삭제합니다.

로그인 유지가 필요해지면 Refresh Token과 HttpOnly·Secure 쿠키 또는 BFF 방식을 별도로 설계합니다. 현재 프론트엔드에는 Refresh Token, 서버 세션과 서버 로그아웃을 포함하지 않습니다.

## 매물·사진·메모

인증 성공 후 `/properties`를 시작 경로로 사용합니다.

| 경로                                               | 기능                                  |
| -------------------------------------------------- | ------------------------------------- |
| `/properties`                                      | 매물 검색·목록·다음 페이지 조회       |
| `/properties/new`                                  | 필수 기본 정보로 매물 등록            |
| `/properties/:propertyId`                          | 상세·메모·요약·삭제 영향 확인         |
| `/properties/:propertyId/edit`                     | 실제 변경 필드만 기본 정보 수정       |
| `/properties/:propertyId/photos`                   | 인증 사진 목록·순차 업로드·삭제       |
| `/properties/:propertyId/active-checklists/:stage` | 단계별 활성 체크리스트 지정·교체·해제 |

현재 매물 화면은 초기 프로토타입의 매물·사진 계약을 사용합니다. 검색별 목록, 상세, 사진 목록과 사진 본문은 인증 Query Cache 아래에서 분리하며 등록·수정·메모·사진·삭제 성공 후 영향받는 query만 갱신하거나 무효화합니다. 401, 토큰 만료와 로그아웃에서는 이 인증 데이터 전체를 제거합니다.

사진 본문 API-203은 JSON이 아닌 비공개 이미지 바이트입니다. `contentUrl`을 `<img src>`로 직접 사용하지 않고 공통 API 클라이언트가 현재 인증 Access Token으로 Blob을 요청합니다. 컴포넌트는 Blob을 Object URL로 변환하고 사진 변경·삭제·unmount에서 즉시 revoke합니다. Blob query의 `gcTime`은 0이며 사진 전체보기는 여섯 장씩 렌더링해 동시에 무제한 요청하지 않습니다.

업로드는 JPEG·PNG·WebP, 파일당 10MiB, 매물당 30장 제한을 선택 시 먼저 안내합니다. 여러 파일을 선택해도 API-202의 `file` 파트 단건 요청을 순차 실행하며 한 장이 실패해도 나머지는 계속 처리합니다. `Content-Type`과 multipart boundary는 브라우저가 설정합니다. 매물과 사진은 네이티브 dialog에서 영향을 확인한 뒤 삭제하며 실패 전에는 화면에서 먼저 제거하지 않습니다.

매물 상세의 퀵 메모는 API-103의 일곱 구조화 필드와 추가 메모를 하나의 자유 형식 텍스트로 합쳐 초기화합니다. 사용자가 저장하면 구조화 필드는 빈 문자열로 정리하고 자유 메모를 `additionalMemo`에 보냅니다. 메모는 5,000 Unicode 코드포인트 이하로 검사하며 초과 입력을 잘라내지 않고 입력란 가까이에 오류를 표시합니다.

입력마다 자동 저장하지 않고 명시적인 `메모 저장`으로 API-106에 여덟 필드를 모두 보냅니다. `content`와 `expectedVersion`은 보내지 않습니다. 저장 성공 응답은 상세 Query Cache의 메모와 최근 활동 시각에 반영하고 목록 Query를 무효화합니다. 저장 실패나 작성 중 상세 재조회에도 로컬 입력을 유지하며 사용자가 명시적으로 다시 저장할 수 있습니다.

## 체크리스트와 매물 연결

체크리스트 경로는 인증된 앱 셸 안에서만 접근합니다.

| 경로                                               | 기능                                      |
| -------------------------------------------------- | ----------------------------------------- |
| `/checklists`                                      | 온라인·전화, 집에서 확인, 계약 전 선택    |
| `/checklists/:stage`                               | 단계별 내 체크리스트 목록                 |
| `/checklists/new?stage=:stage`                     | 빈 목록·원룸 제공 항목에서 혼합 목록 생성 |
| `/checklists/:checklistId`                         | 이름·혼합 항목·순서를 전체 교체로 수정    |
| `/properties/:propertyId/active-checklists/:stage` | 매물의 단계별 활성 연결 관리              |

현재 화면은 초기 프로토타입의 체크리스트·활성 연결 계약을 사용합니다. 확인 단계는 `ONLINE_PHONE`, `ON_SITE`, `PRE_CONTRACT` 세 가지이며 생성 후 바꾸지 않습니다. 이름은 앞뒤 공백을 제거한 1~50자로 저장하고 같은 단계에서 같은 이름을 허용합니다. PROVIDED와 CUSTOM을 섞은 항목은 한 개 이상이어야 하며 배열 순서를 실제 확인 순서로 사용합니다.

PROVIDED는 현재 단계의 활성 API-301 카탈로그에서만 새로 추가하고 같은 `sourceCheckItemId`를 중복 선택하지 않습니다. 상세에 이미 포함됐지만 활성 카탈로그에서 사라진 PROVIDED는 비활성 안내와 함께 유지·제거·재정렬할 수 있고 신규 항목처럼 다시 추가하지 않습니다. CUSTOM은 전역 검색에 섞이지 않고 현재 체크리스트에만 존재하며, trim한 Unicode 코드포인트 1~200자로 검증합니다. 같은 문구의 CUSTOM은 허용합니다.

편집 화면은 서버 항목과 별개의 클라이언트 키가 있는 초안 모델을 사용합니다. 기존 PROVIDED·CUSTOM의 `checklistItemId`를 상세 응답에서 보존하고, 기존 CUSTOM 질문 수정·신규 CUSTOM 추가·혼합 재정렬 결과를 API-306 전체 `items` 배열에 반영합니다. 저장 성공 전에는 상세 캐시를 낙관적으로 바꾸지 않으며 실패나 재조회가 이름·질문·순서 초안을 덮어쓰지 않습니다. 성공하면 API-304·306 응답을 상세 캐시와 편집 기준선의 정본으로 사용합니다.

하나의 내 체크리스트를 여러 매물에 연결할 수 있으며 매물·단계별 활성 연결은 하나입니다. 원본을 수정하면 연결한 매물에서 같은 원본을 다시 조회합니다. 활성 연결 화면에서 새 체크리스트를 만들면 돌아온 목록에서 선택만 하며, 사용자가 최종 확인하기 전에는 API-401을 호출하지 않습니다. 체크리스트를 삭제하면 현재 활성 연결은 함께 삭제되지만 완료한 방문 기록의 스냅샷은 백엔드 정책에 따라 유지됩니다.

체크 항목 검색·프리셋, 단계별 목록, 상세는 인증 Query Cache 아래에서 분리합니다. 생성·수정·삭제와 연결 변경 성공 후에는 항목 카탈로그 전체가 아니라 체크리스트 집계와 영향받는 매물 상세를 갱신합니다. 서버가 응답하지 않은 임의의 다른 `propertyId`를 직접 캐시에 쓰지 않습니다. 401, 토큰 만료와 로그아웃에서는 다른 인증 데이터와 함께 체크리스트 캐시도 제거합니다.

## 방문 자동 저장과 마이페이지

방문과 회원 경로도 인증된 앱 셸 안에서만 접근합니다.

| 경로                             | 기능                                           |
| -------------------------------- | ---------------------------------------------- |
| `/properties/:propertyId/visits` | 한 매물의 복수 방문 기록과 다음 페이지 조회    |
| `/visits/:visitId`               | 방문 스냅샷 확인·항목 자동 저장·방문 완료      |
| `/me`                            | 현재 회원 정보·주요 기록 이동·탭 인증 로그아웃 |
| `/compare`, `/export`, `/tips`   | 비교표·내보내기·선배 팁 공통 준비 중 안내      |

현재 방문 화면은 초기 프로토타입의 방문 계약을 사용합니다. 방문을 시작할 때 매물의 현재 활성 체크리스트 1~3개를 질문·안내·순서와 함께 독립된 스냅샷으로 복사합니다. 이후 원본을 수정·교체·해제·삭제해도 이미 시작한 방문은 바뀌지 않으며, 삭제된 원본의 `sourceChecklistId: null`과 안내가 없는 `guide: null`을 정상 상태로 표시합니다. 한 매물에는 여러 방문을 만들 수 있고 방문을 삭제하는 기능은 제공하지 않습니다.

방문 항목은 `GOOD`, `CAUTION`, `UNCONFIRMED` 중 하나를 선택하는 즉시 API-504로 저장합니다. 같은 항목의 상태 요청은 직렬화하고 저장 중에도 선택을 잠그지 않으며, 빠른 연속 선택은 첫 응답의 새 `statusVersion` 위에 마지막 사용자 의도를 후속 저장합니다. 같은 상태를 명시적으로 다시 선택해 발생한 version 증가도 반영합니다. 요청은 마지막으로 확인한 서버 `statusVersion`을 `expectedStatusVersion`으로 보내며, 성공 응답의 상태 채널과 집계만 캐시에 반영합니다. 서로 다른 항목의 서버 응답이 네트워크에서 역전되면 더 최신 `statusSavedAt` 집계를 과거 응답으로 되돌리지 않습니다.

각 질문 아래 한 줄 메모는 최대 200 Unicode 코드포인트이며 CR·LF를 제거하고 공백과 빈 문자열을 그대로 보존합니다. 입력 때 로컬 draft를 먼저 바꾸고 마지막 입력 1초 후 API-506을 호출합니다. blur, 방문 단계 전환, 내부 링크 이동, 브라우저 뒤로·앞으로 가기와 방문 완료 전에는 debounce를 기다리지 않고 즉시 flush하며 이미 저장 중인 요청과 그사이에 생긴 최신 draft까지 기다립니다. 같은 항목의 메모 요청만 직렬화하고 상태와 메모 채널은 서로 기다리지 않습니다. 저장 실패 상태는 이후 blur·이동·완료가 자동 재전송하지 않고 사용자가 채널별 `다시 저장`을 명시해야 재시도합니다. dirty·pending 상태에서 새로고침이나 탭 종료를 시도하면 브라우저 경고를 사용하며 별도 인증 우회 저장 경로는 만들지 않습니다.

`VISIT_ITEM_STATUS_VERSION_CONFLICT`와 `VISIT_ITEM_MEMO_VERSION_CONFLICT`는 채널별로 최신 API-503을 조회합니다. 상태는 마지막 선택을, 메모는 로컬 draft를 보존한 채 최신 채널 version으로 최대 한 번 자동 재저장합니다. 재충돌하면 자동 반복을 멈추고 해당 채널의 명시적 재시도 버튼을 표시합니다. 조회와 성공 응답은 채널별 캐시 병합을 거쳐 상태 복구가 메모를, 메모 복구가 상태·집계를 덮어쓰지 않습니다. 일반 네트워크 실패도 사용자 선택과 draft를 유지하며 자동 mutation retry를 사용하지 않습니다.

미확인 항목이 있어도 방문을 완료할 수 있습니다. 완료 확인 뒤 모든 항목의 상태·메모 pending을 flush하고 전부 성공한 경우에만 API-505를 호출합니다. 하나라도 실패하면 `IN_PROGRESS`와 draft를 유지하고 실패 채널에 포커스를 옮겨 다시 저장할 수 있게 합니다. 완료는 취소할 수 없고 최초 `completedAt`을 유지하지만 완료 뒤에도 상태와 메모를 모두 수정할 수 있습니다. 방문 시작·항목 저장·완료 성공 후에는 정확한 방문 목록과 매물 상세를 무효화하고 매물 목록은 서버 정렬을 다시 조회합니다. 방문 상세의 항목 성공 응답은 전체 상세를 다시 조회하지 않고 병합합니다.

마이페이지는 별도 회원 API를 추가하지 않고 보호 경로가 API-002로 확인한 `Member`를 정본으로 사용합니다.

매물·체크리스트·방문·마이페이지 화면 모듈은 `React.lazy`로 경로 단위 분할합니다. 지연 청크에는 로딩 상태와 청크 조회 실패 복구 화면이 있으며 프로덕션 엔트리포인트는 Webpack의 350KiB 예산을 유지합니다. 하단 주요 메뉴는 홈·체크리스트·마이를 제공하고 현재 영역을 `aria-current="page"`로 알립니다.

브라우저 history를 가로채는 방문 초안 flush는 현재 잠긴 React Router의 `unstable_HistoryRouter`와 `UNSAFE_createBrowserHistory`에 의존합니다. React Router를 올리기 전에는 push·replace·back·forward, 차단 뒤 원래 delta 복원, 저장 실패 시 URL·draft 유지, history listener 해제를 실제 브라우저와 테스트에서 다시 감사하고 안정 API 전환 여부를 판단합니다.

## 검사 명령어

```bash
npm run typecheck     # TypeScript 타입 검사
npm run lint          # ESLint 검사
npm run format:check  # Prettier 포맷 검사
npm run test          # 전체 테스트 1회 실행
npm run test:watch    # 개발 중 관련 테스트 감시
npm run build         # 프로덕션 빌드
```

프로젝트의 [`.nvmrc`](./.nvmrc)와 다른 Node.js에서 발생한 엔진 경고는 성공으로 간주하지 않습니다. 브라우저 화면은 운영 엔트리포인트 대신 기존 검증 하네스를 명시적으로 켜서 확인합니다. 이 하네스는 `BROWSER_TEST_HARNESS=true`일 때만 선택되며 프로덕션 빌드에는 포함되지 않습니다.

```bash
nvm use
npm ci
BROWSER_TEST_HARNESS=true npm run dev -- --host 127.0.0.1
```

Google 운영 자격 증명이 있는 로컬 환경에서는 위 OAuth 설정을 맞춰 실제 redirect 왕복을 추가 확인합니다. 자격 증명이 없을 때는 인증을 우회하거나 비밀값을 출력하지 않고 MSW 경계를 사용합니다. 새 백엔드와 연결할 때는 Swagger/OpenAPI를 기준으로 프론트엔드 계약을 다시 검증합니다.

[Frontend CI](../.github/workflows/frontend-ci.yml)는 PR과 `main` push의 프론트엔드 변경에 대해 `npm ci`, 타입·린트·포맷·테스트·빌드를 순서대로 검사합니다. CI의 OAuth 값은 빌드 검증용 가짜 공개 값이며 실제 Google 인증에 사용하지 않습니다.

## 구현 범위와 후속 범위

- `/login`, `/oauth/google/callback`, 보호된 `/`와 알 수 없는 경로 fallback
- Google Authorization Code + PKCE 로그인과 API-001 코드 교환
- 탭 단위 Access Token 복원·만료·401·로그아웃 처리
- API-002 현재 회원 조회와 로딩·네트워크·인증 실패 분리
- 모바일 우선 로그인 화면과 보호된 앱 셸
- 프론트엔드 테스트 및 CI 기반
- API-101~106 매물 목록·등록·상세·수정·삭제·메모
- API-201~204 사진 목록·단건 업로드·인증 Blob 조회·삭제
- 모바일 우선 매물 목록·폼·상세·사진 그리드와 접근 가능한 삭제 확인
- API-301~307 체크 항목 검색·프리셋·내 체크리스트 생성·조회·수정·삭제
- API-401~402 매물 단계별 활성 체크리스트 지정·교체·해제
- PROVIDED·CUSTOM 혼합 체크리스트 생성·수정, 항목 순서 변경과 기존 비활성 PROVIDED 보존
- API-501~506 복수 방문 목록·v1.1 스냅샷·상태·메모 독립 CAS·완료 계약 계층
- 상태 즉시 저장, 메모 1초 debounce, blur·단계·내부 이동·완료 전 flush
- 채널별 409 제어 재적용·명시적 재시도와 완료 후 상태·메모 수정
- `/me` 회원 정보·주요 이동·탭 인증 로그아웃
- `/compare`·`/export`·`/tips` 공통 준비 중 안내 화면
- 홈·체크리스트 하단 메뉴와 보호 경로 단위 코드 분할

두 매물 이상 비교·비교표 계산·실제 파일 내보내기·선배 팁 콘텐츠는 공통 준비 중 안내까지만 제공하고 실제 기능은 현재 범위에 포함하지 않습니다. 방문 삭제·완료 취소, 오프라인 초안 영속 저장, 대표 사진 선택·순서 변경, 이미지 가공, 공개·Presigned 사진 URL과 영속 인증도 후속 Issue에서 구현합니다. 자세한 코드 작성 규칙은 [프론트엔드 컨벤션](./docs/FRONTEND_CONVENTIONS.md)을, 배포 절차는 [프론트엔드 배포](./docs/deployment.md)를 참고합니다.
