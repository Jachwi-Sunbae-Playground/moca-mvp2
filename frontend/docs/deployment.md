# 프론트엔드 배포

- 상태: 동작 중
- 현재 배포 환경: `https://www.jachwi-sunbae.kr`
- 문서 성격: 파생
- 대조 대상: `frontend/webpack.config.js`, 실제 CloudFront·S3·파이프라인 구성

전체 구성과 선택 근거는 [배포 아키텍처 설계](../../docs/operations/deployment-architecture.md)에 있다. 백엔드 배포는 [배포](../../backend/docs/operations/deployment.md)를 참고한다.

## 배포 경로

```text
main 병합
  → CodePipeline(codepipeline-project)
    → Commands 액션
        npm ci && npm run build
        aws s3 sync dist/ s3://techcourse-project-2026/jachwi-sunbae/web/ --delete
        aws cloudfront create-invalidation --paths "/index.html"
  → CloudFront(OAC) → S3
```

백엔드와 **별도 파이프라인**이다. 한쪽 실패가 다른 쪽 배포를 막지 않는다.

## 환경변수는 빌드 타임에 박힌다

`webpack.config.js`의 `DefinePlugin`이 `API_BASE_URL`·`GOOGLE_CLIENT_ID`·`GOOGLE_REDIRECT_URI`를 번들에 박아넣는다. 런타임 설정이 아니므로 **값을 바꾸면 재빌드·재배포해야 한다.**

| 환경변수              | 운영 값                                              |
| --------------------- | ---------------------------------------------------- |
| `API_BASE_URL`        | `https://api.jachwi-sunbae.kr`                       |
| `GOOGLE_CLIENT_ID`    | Google Cloud 콘솔의 웹 클라이언트 ID                 |
| `GOOGLE_REDIRECT_URI` | `https://www.jachwi-sunbae.kr/oauth/google/callback` |

값은 CodePipeline 빌드 액션의 환경변수로 전달한다. 번들에 박혀 브라우저에 그대로 노출되므로 비밀이 아니다. 클라이언트 시크릿은 여기 두지 않는다.

값이 비면 `getPublicConfig()`가 예외를 던져 화면이 뜨지 않는다. 잘못된 값으로 조용히 동작하는 것보다 낫다.

## 캐시 무효화

운영 빌드는 파일명에 `contenthash`를 붙인다.

```
main.3ce7f01e0f4de40f8b0a.js
874.f084accee8510f4e798c.js
assets/jachwi-sunbae-logo.2e4dac46707736dbc407.png
```

내용이 바뀌면 파일명이 바뀌므로 브라우저가 캐시된 옛 파일을 쓰지 않는다. 따라서 배포마다 전체 무효화(`/*`)를 걸 필요가 없다.

**`index.html`만 무효화한다.** 이 파일은 이름이 고정이고 안에 해시가 붙은 파일명을 담고 있어, 이것만 새로 받으면 나머지는 자동으로 새 파일을 가리킨다.

개발 빌드에는 해시를 붙이지 않는다. 파일명이 매번 바뀌면 dev-server의 HMR이 불편하다.

## SPA 폴백

react-router의 클라이언트 라우팅을 쓴다. `/properties/1` 같은 경로는 S3에 실제 객체가 없으므로, CloudFront에서 403·404 응답을 `/index.html`(상태 200)로 매핑해야 한다.

이게 없으면 첫 진입과 새로고침이 깨진다. 구글 콜백 경로 `/oauth/google/callback`도 프론트 라우트다.

## CloudFront origin path

**origin path를 `/jachwi-sunbae/web`으로 지정한다.**

버킷 `techcourse-project-2026`은 여러 팀이 공유하고, 같은 버킷의 `jachwi-sunbae/` 아래에 **비공개 사진 객체**도 있다([ADR-0006](../../backend/docs/adr/0006-use-private-s3-compatible-photo-storage.md)). origin path를 비워 두면 CloudFront가 버킷 전체를 공개하게 되어 사진이 인증 없이 노출된다.

## 확인

```bash
curl -I https://www.jachwi-sunbae.kr
curl -I https://www.jachwi-sunbae.kr/properties
```

둘 다 200이어야 한다. 두 번째가 404면 SPA 폴백이 빠진 것이다.

## 실제 구성

| 항목            | 값                                                                                        |
| --------------- | ----------------------------------------------------------------------------------------- |
| CloudFront 배포 | `E3LI41UZ24V9WD` (`d3ajy5jwv266im.cloudfront.net`)                                        |
| 요금제          | Pay as you go                                                                             |
| 원본            | `techcourse-project-2026.s3.ap-northeast-2.amazonaws.com`, 원본 경로 `/jachwi-sunbae/web` |
| 캐시 정책       | 관리형 `CachingOptimized`                                                                 |
| 파이프라인      | `jachwi-sunbae-web-line`                                                                  |

## apex 도메인은 서비스하지 않는다

`jachwi-sunbae.kr`을 그대로 입력한 사용자는 아무 곳에도 닿지 않는다. 가비아는 apex에 CNAME을 넣을 수 없다.

**가비아 웹 포워딩을 쓰면 안 된다.** 이 기능은 `@`뿐 아니라 `www`에도 가비아 포워딩 서버를 가리키는 A 레코드를 만든다. 한 호스트에 CNAME과 A는 공존할 수 없으므로 `www`의 CloudFront CNAME이 밀려나 **사이트 전체가 뜨지 않게 된다.**

AWS로 리다이렉트를 만들려면 리다이렉트 전용 S3 버킷과 CloudFront 배포, apex를 포함한 인증서가 더 필요하다. 지울 수 없는 리소스가 둘 늘어나므로 지금은 두지 않는다.

## JSX 런타임은 빌드 모드에 따라 갈린다

`webpack.config.js`에서 `@babel/preset-react`에 `development`를 **명시한다.**

```js
['@babel/preset-react', { runtime: 'automatic', development: !isProduction }],
```

명시하지 않으면 babel이 개발 모드로 판단해 `jsxDEV`를 내보낸다. webpack의 `--mode production`은 번들 안의 `NODE_ENV`만 바꾸고 빌드 프로세스의 `NODE_ENV`는 건드리지 않기 때문이다.

React 19의 운영 JSX 런타임에는 `jsxDEV`가 없다. 그래서 **빌드는 성공하고 브라우저에서만 터진다.**

```
Uncaught TypeError: (0 , u.jsxDEV) is not a function
```

빌드·타입·린트·테스트가 모두 통과하므로 CI로는 걸러지지 않는다. 운영 번들을 실제 브라우저에서 열어봐야 드러난다.

## 빌드 환경의 Node 버전

파이프라인은 `.nvmrc`의 버전을 공식 tarball로 내려받아 **절대 경로로 실행한다.**

```bash
NODE_VERSION="$(tr -d '[:space:]' < frontend/.nvmrc | sed 's/^v//')"
...
PATH="/opt/node/bin:$PATH" /opt/node/bin/npm --prefix frontend run build
```

관리형 빌드 환경에는 Node 18이 이미 설치되어 있고 PATH에서 앞선다. `yum install nodejs`로 22를 설치해도 실행되는 것은 18이다. `@babel/core` 8은 ESM 전용이라 `require(esm)`을 지원하지 않는 Node 18에서는 빌드가 실패한다.

```
Error [ERR_REQUIRE_ESM]: require() of ES Module @babel/core/lib/index.js not supported
```

`export PATH`도 안전하지 않다. 빌드 명령이 줄 단위로 실행되므로 앞 줄의 `export`가 다음 줄까지 살아 있다고 가정하지 않는다. 같은 줄에 `PATH=`를 앞세우고 npm도 절대 경로로 부른다.

여러 줄에 걸친 `case`·`if`·`for` 구문도 쓰지 않는다. 줄마다 쪼개져 깨진다.
