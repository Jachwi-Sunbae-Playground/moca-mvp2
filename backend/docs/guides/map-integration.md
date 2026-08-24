# 지도 외부 연동

- 상태: 구현 완료 v1
- 문서 성격: 파생
- 대조 대상: [지도 명세](../../../docs/product/specs/map.md), Kakao Maps 공식 문서

## 선택

- 지도 렌더링: Kakao Maps JavaScript SDK
- 주소 검색·역지오코딩·장소 검색: Kakao Local REST API를 백엔드에서 호출
- 로컬 기본: 외부 호출 없는 `demo`
- 실제 데이터: `live`

Kakao JavaScript SDK는 JavaScript 키와 등록 도메인이 필요하고, Local REST API는 REST API 키가 필요하다. REST 키는 프론트 코드에 넣지 않는다.

## 사용자가 준비할 값

### Kakao Developers

1. Kakao Developers에서 애플리케이션을 만든다.
2. Kakao Maps 사용 설정을 켠다.
3. JavaScript 키에 `http://localhost:3000`을 SDK 도메인으로 등록한다.
4. 실제 배포 뒤 `https://<서비스 도메인>`도 등록한다.
5. JavaScript 키와 REST API 키를 각각 확인한다.
6. 개발자 계정의 첫 번째 활성화 앱인지와 무료 쿼터 뱃지를 확인한다.

2026년 7월 21일 이후 Kakao Maps 무료 쿼터는 개발자 계정의 첫 번째 활성화 앱에만 적용될 수 있다. 비즈월렛·유료 API는 자동 활성화하지 않는다. 쿼터가 없으면 `demo`를 사용하거나 사용자가 비용을 확인한 뒤 별도로 결정한다.

공식 문서:

- [Web API 시작과 도메인 등록](https://apis.map.kakao.com/web/guide/)
- [주소·좌표·장소 REST API](https://developers.kakao.com/docs/ko/local/dev-guide)
- [사용 설정과 쿼터](https://developers.kakao.com/docs/ko/kakaomap/common)

## 환경변수

### 백엔드

| 변수 | `demo` | `live` | 설명 |
| --- | --- | --- | --- |
| `MAP_PROVIDER_MODE` | `demo` | `kakao` | 외부 adapter 선택 |
| `KAKAO_REST_API_KEY` | 불필요 | 필수 | 서버 전용 REST 키 |
| `MAP_CACHE_TTL_SECONDS` | `600` | `600` | 주변 조회 cache TTL |
| `MAP_CONNECT_TIMEOUT_MILLIS` | `2000` | `2000` | 연결 제한 |
| `MAP_READ_TIMEOUT_MILLIS` | `5000` | `5000` | 응답 제한 |

### 프론트엔드

| 변수 | `demo` | `live` | 설명 |
| --- | --- | --- | --- |
| `MAP_PROVIDER_MODE` | `demo` | `kakao` | 지도 component 선택 |
| `KAKAO_MAP_JAVASCRIPT_KEY` | 불필요 | 필수 | 등록 도메인에서 사용하는 JavaScript 키 |

## 로컬 확인

### 키 없이

`MAP_PROVIDER_MODE=demo`로 실행한다. 고정 지도 배경·주소·다섯 카테고리 장소가 동일 API 계약으로 제공되어 위치 선택과 주변 분석을 확인할 수 있다.

### 실제 Kakao

백엔드와 프론트엔드 모두 `kakao` 모드로 맞추고 각 키를 설정한다. 프론트는 JavaScript SDK 키가 없으면 설정 오류 화면을 표시하고, 백엔드는 REST 키가 없으면 시작에 실패한다.

프론트는 Kakao JavaScript SDK로 지도와 핀을 렌더링한다. 백엔드는 주소 검색·역지오코딩·장소 검색을 수행한다. 각 카테고리·키워드 검색은 페이지당 15개씩 응답의 `is_end`까지 최대 세 페이지를 조회하므로 검색 하나당 최대 45개를 정규화한다. `TRANSPORT`는 지하철 카테고리와 `버스정류장` 키워드 결과를 합치며 공급자 장소 ID로 중복을 제거한다.

주변 조회의 `counts`는 Kakao의 전체 검색 건수가 아니라 실제로 정규화해 반환한 장소 수다. 프론트는 이 실제 장소 좌표를 사용하며, 지도 축소 단계에서는 가까운 장소를 좌표 중심 군집으로 묶고 확대 단계 3 이하에서는 개별 핀으로 표시한다. 모든 카테고리를 처음 조회할 때 공급자 호출은 카테고리 다섯 개와 버스정류장 키워드를 합쳐 캐시 미적중 기준 최대 18회이며, 동일한 정규화 좌표·반경·카테고리는 10분 캐시한다.

## 운영 보호

- REST 키·정확한 현재 좌표·전체 Kakao 응답을 로그에 남기지 않는다.
- `live`의 429와 5xx는 503 도메인 오류로 바꾸고 키 값은 응답하지 않는다.
- 좌표·반경·카테고리를 cache key로 사용하되 좌표는 소수점 4자리로 정규화한다.
- cache는 성능 최적화이며 정본이 아니다. 장애 시 오래된 장소를 DB에서 제공하지 않는다.
