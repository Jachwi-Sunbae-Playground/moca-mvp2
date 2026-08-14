# 프론트엔드 배포

- 상태: 구성 중
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

## 아직 구성하지 않은 것

- CloudFront 배포와 프론트 파이프라인은 콘솔에서 만든다.
- apex(`jachwi-sunbae.kr`)는 가비아 웹 포워딩으로 `https://www.jachwi-sunbae.kr`에 리다이렉트한다. 가비아는 apex에 CNAME을 넣을 수 없다.
