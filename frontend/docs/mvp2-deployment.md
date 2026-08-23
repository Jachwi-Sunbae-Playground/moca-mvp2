# MVP2 프론트엔드 배포

- 상태: 저장소 기반 준비 완료, 미배포
- 문서 성격: 파생
- 대조 대상: `frontend/webpack.config.js`, `deploy/Caddyfile.example`, `.github/workflows/deploy-production.yml`

전체 구성은 [MVP2 배포 아키텍처](../../docs/operations/mvp2-deployment-architecture.md)를 따른다.

## 빌드와 서빙

- GitHub Actions가 Node 버전과 `package-lock.json`을 기준으로 `npm ci`, `npm run build`를 실행한다.
- `frontend/dist`를 백엔드 JAR와 같은 릴리스에 묶는다.
- Caddy가 정적 파일을 직접 제공하고 실제 파일이 없는 경로는 `index.html`로 보낸다.
- 프론트와 API가 같은 Origin을 사용하므로 운영 `API_BASE_URL`은 빈 문자열로 빌드한다. API 경로 `/api`는 그대로 Caddy를 거쳐 백엔드에 전달된다.
- 파일명에 `contenthash`가 있으므로 새 릴리스는 새로운 JS·CSS 파일을 사용한다.

## 빌드 타임 공개 설정

| 환경변수                   | 운영 값                                       |
| -------------------------- | --------------------------------------------- |
| `API_BASE_URL`             | 빈 문자열, 같은 Origin 사용                   |
| `AUTH_MODE`                | `google`                                      |
| `GOOGLE_CLIENT_ID`         | GitHub Environment의 공개 OAuth Client ID     |
| `GOOGLE_REDIRECT_URI`      | `https://<공개 도메인>/oauth/google/callback` |
| `MAP_PROVIDER_MODE`        | `kakao`                                       |
| `KAKAO_MAP_JAVASCRIPT_KEY` | GitHub Environment의 공개 Kakao JavaScript 키 |

이 값은 브라우저 번들에서 보이므로 비밀값을 넣지 않는다. Google Client Secret과 Kakao REST API 키는 백엔드 EC2 환경변수에서만 관리한다.

## 확인

첫 배포와 롤백 뒤 다음을 확인한다.

```bash
curl --fail --head https://<공개 도메인>/
curl --fail --head https://<공개 도메인>/properties
```

두 경로가 정적 HTML로 응답하고 브라우저에서 최신 번들이 로드되어야 한다. `/properties`가 404면 Caddy의 `try_files` SPA fallback을 확인한다. OAuth callback 직접 진입과 `/api` 요청도 함께 점검한다.
