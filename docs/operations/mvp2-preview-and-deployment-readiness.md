# MVP2 로컬 확인과 배포 준비

- 상태: 로컬 구현 완료·실연동 미검증
- 문서 성격: 파생
- 대조 대상: [MVP2 구현 인계서](../product/mvp2-implementation-brief.md), `backend/.env.example`, `frontend/.env.example`, `deploy/`

## 구현 경계

MVP2의 로컬 실행과 배포 설정 경계까지 구현했다. AWS 리소스 생성, 비용 발생, DNS 변경과 실제 배포는 사용자가 MVP2를 확인한 뒤 별도로 요청할 때만 한다.

## 키 없이 확인 가능한 범위

- 데모 로그인
- 데모 회원의 매물·사진·메모·체크리스트 CRUD와 상태·메모 자동 저장
- 17개 기준 화면과 빈·로딩·오류 상태
- 데모 지도, 주소 선택과 다섯 주변 카테고리
- 2~5개 매물 선택과 저장 기록 전체 PDF 비교표
- MySQL 영속화와 MinIO 사진 저장
- Swagger UI

실행 명령과 포트는 [로컬 개발](../../backend/docs/guides/local-development.md), 변수의 정본은 [환경변수](../../backend/docs/guides/environment-variables.md)를 따른다. 데모 로그인은 암호 없이 `demo@moca.local` 회원을 사용한다.

## 2026-08-24 로컬 검증 결과

- Docker MySQL과 MinIO가 healthy인 상태에서 백엔드와 프론트엔드를 함께 실행했고 `/actuator/health`의 `UP`과 실제 데모 API 연결을 확인했다.
- 백엔드 전체 테스트, 프론트엔드 22개 파일·132개 테스트, typecheck·lint·format·production build와 문서 정합성 검사를 실행했다.
- `390x844` 브라우저 viewport에서 `00`부터 `10`, `13-1`, `13-2`까지 16개 화면과 데모 로그인·매물 생성·조회·사진·메모·체크리스트·지도 위치 선택을 확인했다.
- 브라우저 제어 보안 검토가 이후 localhost 접근을 차단해 `13-3`의 실제 브라우저 진입과 두 번째 모바일 폭 확인은 수행하지 못했다. `13-3`의 기본 2km·반경 변경·카테고리 빈 상태는 독립 화면 테스트로 검증했다.
- Google OAuth, Kakao 실제 SDK·Local API와 AWS S3는 키·외부 자원을 만들지 않았으므로 미검증이다.

## 2026-08-25 비교 PDF 검증 결과

- 매물 목록의 1·2·3단계별 집계와 발견 경로, 2~5개 선택 화면을 프론트엔드 테스트로 확인했다.
- 한글 글꼴을 임베딩한 샘플 PDF를 생성해 모든 5페이지를 이미지로 렌더링했고, 가로형 요약표·사진·메모·세 단계 질문과 항목 메모가 잘림 없이 보이는지 확인했다.

## 실제 외부 연동에 필요한 사용자 작업

### Google 로그인

- Google Cloud에서 Web OAuth client를 만든다.
- 로컬 redirect URI `http://localhost:3000/oauth/google/callback`을 등록한다.
- 운영 redirect URI `https://<서비스 도메인>/oauth/google/callback`을 등록한다.
- 프론트엔드 client ID와 백엔드 client ID·secret을 환경변수에 넣는다.

### Kakao 지도

- [지도 외부 연동](../../backend/docs/guides/map-integration.md)에 따라 앱·JavaScript 도메인·키를 준비한다.
- 무료 쿼터 적용 여부를 확인하고 유료 API는 별도로 승인하기 전 활성화하지 않는다.

### S3 사진

- 비공개 S3 버킷을 만든다.
- EC2 instance role 또는 최소 권한 IAM principal에 해당 버킷의 읽기·쓰기·삭제 권한만 준다.
- 운영에서는 장기 access key보다 EC2 role을 우선한다.
- CORS 공개 읽기를 열지 않고 사진은 백엔드 인증 endpoint로 제공한다.

## 실제 배포 전 준비값

| 구분 | 필요한 값 |
| --- | --- |
| 네트워크 | EC2 public IP, 서비스 도메인, DNS A/AAAA |
| HTTPS | Caddy가 인증서를 발급할 수 있는 80·443 inbound |
| DB | 운영 MySQL 사용자·비밀번호, 백업 경로와 복구 확인 |
| 인증 | Google client ID·secret·redirect URI |
| 지도 | Kakao JavaScript key·REST key·운영 SDK 도메인, 실제 버스정류소용 공공데이터포털 TAGO 일반 인증키(Decoding) |
| 사진 | S3 region·bucket·key prefix·EC2 role |
| 애플리케이션 | JWT secret, CORS origin, 운영 profile |
| GitHub | 배포 대상·SSH 또는 SSM 접근과 필요한 Actions secret |

### 구현과 맞춘 값 이름

- 백엔드 Google: `AUTH_MODE=google`, `GOOGLE_OAUTH_CLIENT_ID`, `GOOGLE_OAUTH_CLIENT_SECRET`
- 프론트 Google: `AUTH_MODE=google`, `GOOGLE_CLIENT_ID`, `GOOGLE_REDIRECT_URI`
- 백엔드 Kakao: `MAP_PROVIDER_MODE=kakao`, `KAKAO_REST_API_KEY`
- 백엔드 버스정류소: `BUS_STOP_PROVIDER=tago`, `DATA_GO_KR_SERVICE_KEY`
- 프론트 Kakao: `MAP_PROVIDER_MODE=kakao`, `KAKAO_MAP_JAVASCRIPT_KEY`
- 운영 S3: `PHOTO_STORAGE_REGION`, `PHOTO_STORAGE_BUCKET`, `PHOTO_STORAGE_KEY_PREFIX`; endpoint와 정적 access key는 비우고 EC2 role을 사용한다.
- 공통: `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, DB 접속값

비밀값은 저장소에 커밋하지 않는다.

## 배포 직전 체크리스트

- [ ] 병합된 `main`에서 로컬 `demo` 전체 흐름을 다시 확인한다.
- [ ] 로컬 `live`에서 Google·Kakao를 확인한다.
- [ ] S3 테스트 객체 업로드·조회·삭제가 성공한다.
- [ ] 운영 schema 초기화와 seed 범위를 검토한다.
- [ ] 운영 DB 백업과 한 번의 복구 연습을 완료한다.
- [ ] Google redirect URI와 Kakao JavaScript SDK 도메인에 실제 HTTPS 도메인을 등록한다.
- [ ] Kakao 무료 쿼터와 과금 비활성 상태를 확인한다.
- [ ] 국토교통부 TAGO 버스정류소정보를 활용 신청하고 중심 500m 실제 정류소를 확인한다.
- [ ] Caddy health check와 백엔드 `/actuator/health`를 확인한다.
- [ ] GitHub Actions 수동 배포가 CI 전체 검사를 반복하지 않고 빌드 산출물을 전달하도록 확인한다.
- [ ] 롤백할 이전 release와 DB 백업 위치를 확인한다.

## 배포 후 확인 순서

1. HTTPS와 health endpoint
2. Google 로그인과 마이페이지
3. 매물 생성·수정·삭제
4. 사진 S3 업로드·대표 변경·삭제
5. 메모와 체크 상태 자동 저장
6. 지도 현재 위치·주소·주변 분석
7. 로그에 token·key·정확한 좌표가 없는지 확인
8. 배포 workflow와 이전 release 롤백 확인
