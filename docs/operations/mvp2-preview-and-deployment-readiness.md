# MVP2 로컬 확인과 배포 준비

- 상태: 구현 목표 v1
- 대조 대상: [MVP2 구현 인계서](../product/mvp2-implementation-brief.md), `backend/.env.example`, `frontend/.env.example`, `deploy/`

## 이번 단계의 경계

다음 구현 작업은 로컬 완성과 배포 준비까지 수행한다. AWS 리소스 생성, 비용 발생, DNS 변경과 실제 배포는 사용자가 MVP2를 확인한 뒤 별도로 요청할 때만 한다.

## 사용자가 키 없이 확인할 수 있어야 하는 것

- 데모 로그인
- 데모 회원의 매물·사진·메모·체크리스트 CRUD
- 17개 기준 화면과 빈·로딩·오류 상태
- 데모 지도, 주소 선택과 다섯 주변 카테고리
- CSV 비교표
- MySQL 영속화와 MinIO 사진 저장
- Swagger UI

구현 완료 후 저장소의 한 안내 절차만 따라 MySQL·MinIO·백엔드·프론트엔드를 실행할 수 있어야 한다.

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
| 지도 | Kakao JavaScript key·REST key·운영 SDK 도메인 |
| 사진 | S3 region·bucket·key prefix·EC2 role |
| 애플리케이션 | JWT secret, CORS origin, 운영 profile |
| GitHub | 배포 대상·SSH 또는 SSM 접근과 필요한 Actions secret |

비밀값은 저장소에 커밋하지 않는다.

## 배포 직전 체크리스트

- [ ] 로컬 `demo` 전체 흐름이 통과한다.
- [ ] 로컬 `live`에서 Google·Kakao를 확인한다.
- [ ] S3 테스트 객체 업로드·조회·삭제가 성공한다.
- [ ] 운영 schema 초기화와 seed 범위를 검토한다.
- [ ] 운영 DB 백업과 한 번의 복구 연습을 완료한다.
- [ ] Google redirect URI와 Kakao JavaScript SDK 도메인에 실제 HTTPS 도메인을 등록한다.
- [ ] Kakao 무료 쿼터와 과금 비활성 상태를 확인한다.
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
