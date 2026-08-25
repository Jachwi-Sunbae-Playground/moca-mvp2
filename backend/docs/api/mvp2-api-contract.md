# MVP2 API 계약

- 상태: 구현 완료 v1
- 문서 성격: 파생
- 대조 대상: [MVP2 기능 명세](../../../docs/product/specs/README.md), 실제 Spring MVC 컨트롤러와 `/v3/api-docs`

## 공통

- 경로 prefix는 `/api`다.
- 보호 API는 `Authorization: Bearer <access-token>`을 요구한다.
- 성공 envelope는 기존 `ApiResponse`, 오류는 기존 `DomainErrorResponse`를 유지한다.
- 다른 회원의 자원은 404로 처리한다.
- 날짜·시간은 ISO 8601 UTC, 금액은 원 단위 정수, 좌표는 JSON number다.

## 인증·회원

| 메서드 | 경로 | 인증 | 설명 |
| --- | --- | --- | --- |
| `POST` | `/api/auth/nickname` | 공개 | 닉네임과 선택 비밀번호로 시작 |
| `GET` | `/api/members/me` | 필요 | 현재 회원 조회 |

로그인 요청은 `nickname`과 생략 가능한 `password`를 받는다. 처음 보는 닉네임이면 비밀번호 유무에 따라 공유 또는 보호 회원을 만들고, 기존 닉네임이면 같은 회원으로 연결한다.

```json
{
  "nickname": "이자취",
  "password": "선택 입력"
}
```

로그인 응답은 `accessToken`, `tokenType`, `expiresIn`과 `member.memberId`, `member.name`, `member.passwordProtected`를 제공한다. `/api/members/me`도 `memberId`, `displayName`, `passwordProtected`만 반환하며 내부 식별용 이메일은 노출하지 않는다.

## 매물

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `GET` | `/api/properties` | 최근 활동순 목록과 대표 사진·사진 수·1·2·3단계별 진행 현황 |
| `GET` | `/api/properties/export.csv` | 호환용 UTF-8 BOM 매물 요약 다운로드 |
| `POST` | `/api/properties/export.pdf` | 선택한 2~5개 매물의 전체 기록 PDF 다운로드 |
| `POST` | `/api/properties` | 매물 생성 |
| `GET` | `/api/properties/{propertyId}` | 상세 조회 |
| `PUT` | `/api/properties/{propertyId}` | 기본 정보 전체 교체 |
| `DELETE` | `/api/properties/{propertyId}` | 종속 데이터와 객체 사진 삭제 |

생성·수정 요청의 목표 필드는 다음과 같다.

```json
{
  "name": "신림역 원룸",
  "roadAddress": "서울 관악구 신림로 12길 3",
  "jibunAddress": "서울 관악구 신림동 123-4",
  "latitude": 37.4841234,
  "longitude": 126.9291234,
  "depositAmount": 10000000,
  "monthlyRentAmount": 550000,
  "discoverySource": "https://example.com/property/1"
}
```

목록·상세 응답은 `address`, `roadAddress`, `jibunAddress`, `latitude`, `longitude`, `photoCount`, `representativePhoto`, `overallProgress`, `lastActivityAt`을 제공한다. 목록은 추가로 `stages`에 `ONLINE_PHONE`, `ON_SITE`, `PRE_CONTRACT` 순서의 `applied`와 단계별 `progress`를 제공한다. `address`는 도로명 주소가 있으면 그 값을, 없으면 지번 주소를 담는 표시용 파생 필드다.

비교 PDF 요청은 서로 다른 소유 매물 ID 2~5개를 선택 순서로 보낸다.

```json
{
  "propertyIds": [12, 7, 31]
}
```

성공은 `application/pdf`와 attachment 파일을 반환한다. PDF의 첫 페이지는 나란한 기본 정보·단계 집계이고, 이후는 매물별 기본 정보·모든 사진·구조화 메모·자유 메모·적용한 모든 체크 질문과 상태·항목 메모를 담는다. PDF에 넣는 사진은 메모리와 파일 크기를 제한하도록 긴 변 1,200px 이하 JPEG로 변환하며 객체 저장소의 원본은 변경하지 않는다. 2~5개 범위나 중복 위반은 400, 소유하지 않은 매물은 404다.

## 사진

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `GET` | `/api/properties/{propertyId}/photos` | 사진 목록 |
| `POST` | `/api/properties/{propertyId}/photos` | `multipart/form-data`의 `file` 업로드 |
| `GET` | `/api/properties/{propertyId}/photos/{photoId}` | 인증된 사진 콘텐츠 스트림 |
| `DELETE` | `/api/properties/{propertyId}/photos/{photoId}` | 사진 삭제 |
| `PUT` | `/api/properties/{propertyId}/photos/{photoId}/representative` | 대표 지정 |

업로드 성공은 201과 새 사진 메타데이터를 반환한다. 크기·형식·개수 위반은 400, 소유자가 아니면 404다.
서버는 선언된 MIME뿐 아니라 실제 이미지 형식도 확인한다. 저장소 업로드 뒤 DB 저장이 실패하면 같은 객체 key를 보상 삭제한다.

## 메모

기존 `GET`, `POST`, `PUT /api/properties/{propertyId}/memo` 계약을 유지한다. `POST`는 최초 스냅샷 초기화, `PUT`은 전체 저장이다.

## 시스템·사용자 체크리스트

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `GET` | `/api/check-items?stage=&query=` | 공개 시스템 체크 항목 검색 |
| `GET` | `/api/system-memo-items` | 활성 시스템 메모 항목 |
| `GET` | `/api/checklists?stage=` | 내 체크리스트 목록 |
| `POST` | `/api/checklists` | 생성 |
| `GET` | `/api/checklists/{checklistId}` | 상세 |
| `PUT` | `/api/checklists/{checklistId}` | 이름·전체 항목 교체 |
| `DELETE` | `/api/checklists/{checklistId}` | 삭제 |

## 매물 적용 체크리스트

`PUT /api/properties/{propertyId}/checklists/{stage}` 요청은 사용자 또는 가상 기본체크리스트를 구분한다.

```json
{ "sourceType": "USER", "checklistId": 12 }
```

```json
{ "sourceType": "SYSTEM_DEFAULT", "checklistId": null }
```

기존 조회·상태·메모 endpoint는 유지한다.

- `GET /api/properties/{propertyId}/checklists`
- `GET /api/properties/{propertyId}/checklists/{propertyChecklistId}`
- `PATCH /api/properties/{propertyId}/checklists/{propertyChecklistId}/items/{itemId}/status`
- `PATCH /api/properties/{propertyId}/checklists/{propertyChecklistId}/items/{itemId}/memo`

## 지도·주소

| 메서드 | 경로 | 인증 | 설명 |
| --- | --- | --- | --- |
| `GET` | `/api/maps/geocode?query=` | 필요 | 주소 검색과 좌표 후보 |
| `GET` | `/api/maps/reverse-geocode?latitude=&longitude=` | 필요 | 좌표의 도로명·지번 주소 |
| `GET` | `/api/maps/nearby?latitude=&longitude=&radius=&categories=` | 필요 | 주변 장소와 카테고리 집계 |

`categories`는 `HOSPITAL,TRANSPORT,SCHOOL,CONVENIENCE,AGENCY`의 쉼표 목록이며 생략하면 전체다. `radius`는 500·1000·2000만 허용한다.

`places`는 Kakao 카테고리 검색별로 페이지당 15개, 최대 세 페이지를 정규화한 결과다. `TRANSPORT`는 Kakao 지하철을 기본으로 반환하고, TAGO를 설정했으면 조회 중심 500m 안의 실제 버스정류소를 합친 뒤 공급자 ID로 중복을 제거한다. `counts`는 공급자의 전체 검색 건수가 아니라 응답 `places`의 카테고리별 개수다. TAGO 장애 시 버스정류소만 제외하고 Kakao 결과는 계속 반환한다.

주변 조회 응답의 형태는 다음과 같다.

```json
{
  "center": { "latitude": 37.5879, "longitude": 126.9936 },
  "radius": 2000,
  "counts": {
    "HOSPITAL": 6,
    "TRANSPORT": 8,
    "SCHOOL": 3,
    "CONVENIENCE": 8,
    "AGENCY": 4
  },
  "places": [
    {
      "providerPlaceId": "123",
      "name": "예시 병원",
      "category": "HOSPITAL",
      "address": "서울 종로구 ...",
      "latitude": 37.58,
      "longitude": 126.99,
      "distanceMeters": 420
    }
  ]
}
```

## 신규 오류 코드

| 코드 | 상태 | 의미 |
| --- | --- | --- |
| `NICKNAME_INVALID` | 400 | 닉네임 길이·제어 문자·정규화 오류 |
| `NICKNAME_PASSWORD_INVALID` | 400 | 선택 비밀번호 길이 또는 바이트 제한 위반 |
| `NICKNAME_PASSWORD_UNEXPECTED` | 409 | 공유 닉네임을 비밀번호로 선점하려 함 |
| `NICKNAME_AUTHENTICATION_FAILED` | 401 | 보호 닉네임 비밀번호 불일치 |
| `NICKNAME_AUTH_RATE_LIMITED` | 429 | 닉네임별 반복 실패 제한 초과 |
| `PROPERTY_LOCATION_INVALID` | 400 | 주소·좌표 조합 또는 범위 오류 |
| `PHOTO_LIMIT_EXCEEDED` | 400 | 사진 30장 초과 |
| `PHOTO_CONTENT_TYPE_UNSUPPORTED` | 400 | 미지원 형식 |
| `PHOTO_SIZE_EXCEEDED` | 400 | 5MiB 초과 |
| `MAP_QUERY_INVALID` | 400 | 좌표·반경·카테고리 오류 |
| `MAP_PROVIDER_UNAVAILABLE` | 503 | Kakao 장애·429·타임아웃 |

## 정합성 확인

- Swagger UI와 `/v3/api-docs`는 실행 중인 컨트롤러에서 생성된다.
- 통합 테스트가 닉네임 로그인부터 주소·매물·메모·체크·사진·PDF·지도·삭제까지 실제 HTTP 계약을 검증한다.
- 프론트 DTO parser와 MSW handler는 같은 응답 계약을 사용한다.
