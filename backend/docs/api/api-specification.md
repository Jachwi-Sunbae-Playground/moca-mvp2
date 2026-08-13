# API 명세

- 상태: 1차 MVP v1.1 백엔드 최종 기준선
- 현재 등록된 비즈니스 API: 27개
- 구현 확인: Swagger UI `/swagger-ui/index.html`, OpenAPI JSON `/v3/api-docs`
- 프론트엔드 구현 정본: 이 문서의 개별 API 절, [구현 추적](#구현-추적), [프론트엔드-v11-인계-기준선](#프론트엔드-v11-인계-기준선)

목표 제품 의미는 Notion의 [요구사항](https://www.notion.so/3b6eba944b438002b8dfe971b31c1236), [도메인](https://www.notion.so/3b8eba944b43806eae4ed2034748ab8d), [API](https://www.notion.so/3b8eba944b43803887a8c0aac5784ac2), [DB](https://www.notion.so/3b8eba944b438014bc58ecb0835e4a79), [Java](https://www.notion.so/3b8eba944b4380c489e2fa251b7edf5a), [TODO](https://www.notion.so/3b8eba944b4380e2bbdafe442194aa1d) 설계와 대조했다. 현재 구현 사실과 프론트엔드 계약은 코드와 함께 검증하는 이 문서를 우선한다.

## 인증

- 비즈니스 API 중 `POST /api/auth/google`만 공개한다.
- `GET /api/members/me`와 모든 매물·체크리스트·방문 API는 `Authorization: Bearer {accessToken}`을 요구한다.
- Access Token은 HS256, `iss=jachwi-sunbae`, `aud=jachwi-sunbae-api`, 내부 `memberId` subject와 기본 12시간 TTL을 사용한다.
- 서버 세션, Refresh Token과 서버 로그아웃 API는 사용하지 않는다.
- 프론트엔드는 Access Token을 메모리에만 보관하며 로그아웃할 때 메모리에서 제거한다.
- Google code, code verifier, nonce, Google token과 자취선배 JWT는 URL, 오류 응답과 로그에 남기지 않는다.

## 공통 성공 응답

```json
{
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "data": {}
}
```

## 공통 오류 응답

```json
{
  "code": "ACCESS_TOKEN_INVALID",
  "message": "Access Token이 올바르지 않습니다.",
  "errors": []
}
```

검증 오류의 `errors`에는 필드, 거절된 값과 사유를 기록한다. 민감정보는 포함하지 않는다.

예외 계층과 상태 코드 매핑은 [예외 컨벤션](../conventions/exception-convention.md)을 따른다.

## API-001 Google 코드 교환 로그인

- 목적: Google authorization code를 검증하고 자취선배 JWT Access Token을 발급한다.
- Method: `POST`
- URL: `/api/auth/google`
- 인증: 불필요
- 성공 상태: `200 OK`
- 대표 오류: `INVALID_REQUEST`, `GOOGLE_AUTHORIZATION_CODE_INVALID`, `GOOGLE_IDENTITY_INVALID`, `GOOGLE_AUTHENTICATION_FAILED`

```json
{
  "authorizationCode": "google-one-time-code",
  "codeVerifier": "pkce-code-verifier-with-43-to-128-characters",
  "nonce": "oidc-nonce",
  "redirectUri": "https://app.example.com/oauth/google/callback"
}
```

```json
{
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "data": {
    "accessToken": "eyJ...",
    "tokenType": "Bearer",
    "expiresIn": 43200,
    "member": {
      "memberId": 1,
      "displayName": "이자취",
      "email": "jachwi.sunbae@gmail.com"
    }
  }
}
```

- `codeVerifier`는 PKCE 규격에 따라 43자 이상 128자 이하의 unreserved 문자만 허용한다.
- `redirectUri`는 서버 설정의 정확한 허용 목록에 포함돼야 한다.
- 백엔드는 Google token endpoint에서 코드를 교환한 뒤 ID Token의 서명, issuer, audience, 만료, nonce, subject와 검증된 email claim을 확인한다.
- 같은 Google subject의 재로그인은 기존 회원 프로필과 마지막 로그인 시각을 갱신한다.
- 표시 이름이 없으면 `자취생`을 사용하고 100자를 넘으면 100자로 제한한다.
- Google 외부 HTTP 호출과 JWK 처리는 회원 저장 트랜잭션 밖에서 수행한다.

## API-002 현재 사용자 조회

- 목적: 현재 Access Token의 회원 프로필을 조회한다.
- Method: `GET`
- URL: `/api/members/me`
- 인증: 필요
- 성공 상태: `200 OK`
- 대표 오류: `UNAUTHENTICATED`, `ACCESS_TOKEN_EXPIRED`, `ACCESS_TOKEN_INVALID`

```json
{
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "data": {
    "memberId": 1,
    "displayName": "이자취",
    "email": "jachwi.sunbae@gmail.com"
  }
}
```

- 검증된 JWT subject만 회원 식별자로 사용한다.
- 토큰 subject가 존재하지 않는 회원을 가리키면 `ACCESS_TOKEN_INVALID`를 반환한다.

## 매물 입력 규칙

| 필드 | 규칙 |
| --- | --- |
| `name` | 앞뒤 공백 제거 후 1자 이상 50자 이하 |
| `depositAmount` | 0 이상 9,007,199,254,740,991 이하 정수 |
| `monthlyRentAmount` | 0 이상 9,007,199,254,740,991 이하 정수 |
| `discoverySource` | 앞뒤 공백 제거 후 1자 이상 500자 이하인 URL 또는 일반 텍스트 |
| `content` | 0자 이상 5,000자 이하인 매물 메모. 빈 문자열은 메모 지우기 |

- 발견 경로는 host가 있는 HTTP·HTTPS URI만 `URL`로 분류하고 나머지는 `TEXT`로 보존한다.
- 회원 ID는 요청에서 받지 않고 검증된 JWT subject만 사용한다.
- 존재하지 않거나 다른 회원이 소유한 매물은 모두 `PROPERTY_NOT_FOUND`로 응답한다.

## API-101 내 매물 목록 조회

- Method: `GET`
- URL: `/api/properties?query=신림&page=0&size=20`
- 인증: 필요
- 성공 상태: `200 OK`
- 대표 오류: `INVALID_REQUEST`, `INVALID_PAGE_REQUEST`
- `query`는 선택이며 trim 후 0~50자 매물 이름 부분 일치 검색이다.
- `page` 기본값은 0, `size` 기본값은 20이며 1~100만 허용한다.
- `lastActivityAt DESC, propertyId DESC` 순으로 정렬한다.
- 결과가 없으면 `content: []`인 정상 응답을 반환한다.

```json
{
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "data": {
    "content": [
      {
        "propertyId": 10,
        "name": "신림역 원룸",
        "depositAmount": 10000000,
        "monthlyRentAmount": 550000,
        "discoverySource": {
          "type": "URL",
          "value": "https://example.com/listings/10"
        },
        "recentVisit": {
          "visitId": 31,
          "status": "COMPLETED",
          "startedAt": "2026-08-09T05:20:00Z",
          "completedAt": "2026-08-09T06:00:00Z",
          "summary": {
            "totalCount": 22,
            "checkedCount": 15,
            "goodCount": 10,
            "cautionCount": 5,
            "unconfirmedCount": 7
          }
        },
        "photoCount": 0,
        "lastActivityAt": "2026-08-10T07:30:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "hasNext": false
  }
}
```

`photoCount`는 해당 매물의 실제 사진 메타데이터 수다. `recentVisit`은 `startedAt DESC, visitId DESC` 첫 방문이며 방문이 없으면 `null`이다.

## API-102 매물 등록

- Method: `POST`
- URL: `/api/properties`
- 인증: 필요
- 성공 상태: `201 Created`
- `Location`: `/api/properties/{propertyId}`
- 대표 오류: `INVALID_REQUEST`

```json
{
  "name": "신림역 원룸",
  "depositAmount": 10000000,
  "monthlyRentAmount": 550000,
  "discoverySource": "https://example.com/listings/10"
}
```

```json
{
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "data": {
    "propertyId": 10,
    "name": "신림역 원룸",
    "depositAmount": 10000000,
    "monthlyRentAmount": 550000,
    "discoverySource": {
      "type": "URL",
      "value": "https://example.com/listings/10"
    },
    "createdAt": "2026-08-10T07:30:00Z"
  }
}
```

## API-103 매물 상세 조회

- Method: `GET`
- URL: `/api/properties/{propertyId}`
- 인증: 필요
- 성공 상태: `200 OK`
- 대표 오류: `PROPERTY_NOT_FOUND`

```json
{
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "data": {
    "propertyId": 10,
    "name": "신림역 원룸",
    "depositAmount": 10000000,
    "monthlyRentAmount": 550000,
    "discoverySource": {
      "type": "URL",
      "value": "https://example.com/listings/10"
    },
    "memo": {
      "viewingSchedule": "8월 20일 오후 2시 방문",
      "moveInAvailability": "9월 1일부터 입주 가능",
      "provisionalDeposit": "가계약금 30만 원",
      "roomOptions": "냉장고와 세탁기 포함",
      "maintenanceAndUtilities": "관리비와 전기·가스 별도",
      "commuteTime": "학교까지 버스로 20분",
      "governmentSupport": "중소기업 청년 대출 가능 여부 확인",
      "additionalMemo": "채광과 골목 소음을 다시 확인",
      "content": "채광과 골목 소음을 다시 확인",
      "savedAt": "2026-08-10T07:40:00Z"
    },
    "activeChecklists": [],
    "recentVisit": {
      "visitId": 31,
      "status": "COMPLETED",
      "startedAt": "2026-08-09T05:20:00Z",
      "completedAt": "2026-08-09T06:00:00Z",
      "summary": {
        "totalCount": 22,
        "checkedCount": 15,
        "goodCount": 10,
        "cautionCount": 5,
        "unconfirmedCount": 7
      }
    },
    "photoPreview": {
      "totalCount": 2,
      "photos": [
        {
          "photoId": 81,
          "contentUrl": "/api/properties/10/photos/81/content",
          "createdAt": "2026-08-10T07:35:00Z"
        }
      ]
    },
    "deletionImpact": {
      "visitCount": 2,
      "photoCount": 2,
      "activeChecklistCount": 0
    },
    "createdAt": "2026-08-10T07:30:00Z",
    "updatedAt": "2026-08-10T07:30:00Z",
    "lastActivityAt": "2026-08-10T07:30:00Z"
  }
}
```

`photoPreview.totalCount`와 `deletionImpact.photoCount`는 실제 사진 수다. 미리보기는 별도 썸네일을 만들지 않고 업로드 순 첫 사진 한 장의 인증 본문 URL을 제공한다. `activeChecklists`는 연결된 단계만 `ONLINE_PHONE`, `ON_SITE`, `PRE_CONTRACT` 순으로 반환하고 `deletionImpact.activeChecklistCount`는 실제 연결 수다. `recentVisit`은 가장 최근 시작한 방문의 현재 집계이며 `deletionImpact.visitCount`는 전체 방문 수다.

`memo`는 일곱 구조화 필드와 `additionalMemo`, v1.0 호환용 deprecated `content`, `savedAt`을 반환한다. `content`는 항상 `additionalMemo`와 같다. 구조화 메모 행이 없으면 구조화 필드는 빈 문자열, 추가 메모 두 필드는 `properties.memo`, 저장 시각은 `COALESCE(properties.memo_updated_at, properties.updated_at)`으로 복구한다. 조회는 read-repair를 수행하지 않는다.

## API-104 매물 기본 정보 변경

- Method: `PATCH`
- URL: `/api/properties/{propertyId}`
- 인증: 필요
- 성공 상태: `200 OK`
- 대표 오류: `INVALID_REQUEST`, `PROPERTY_NOT_FOUND`
- `name`, `depositAmount`, `monthlyRentAmount`, `discoverySource` 중 하나 이상을 보낸다.
- 누락 필드는 유지하며 명시적인 `null`은 허용하지 않는다.

```json
{
  "name": "신림역 원룸 2차 방문",
  "monthlyRentAmount": 530000
}
```

성공 응답은 변경 후 전체 기본 정보와 `updatedAt`을 반환한다.

## API-105 매물 삭제

- Method: `DELETE`
- URL: `/api/properties/{propertyId}`
- 인증: 필요
- 성공 상태: `204 No Content`
- 대표 오류: `PROPERTY_NOT_FOUND`, `PHOTO_DELETE_FAILED`
- 응답 본문 없이 매물을 물리 삭제한다.
- 현재 사진 객체를 모두 삭제하거나 이미 없음을 확인한 뒤 짧은 DB 트랜잭션에서 매물 행을 잠근다.
- 잠금 뒤 새 사진을 다시 확인하고 새 객체가 있으면 외부 삭제를 반복한다. 최대 3회 안에 수렴하지 않거나 외부 삭제가 하나라도 실패하면 DB 매물을 유지하고 `PHOTO_DELETE_FAILED`를 반환한다.
- DB 매물 삭제가 커밋되면 현재 활성 체크리스트 연결과 방문·단계 스냅샷·방문 항목도 FK cascade로 제거한다. 회원 체크리스트 자체와 다른 매물의 연결은 유지하며 `assignedPropertyCount`는 감소한다.

## API-106 매물 메모 저장

- Method: `PUT`
- URL: `/api/properties/{propertyId}/memo`
- 인증: 필요
- 성공 상태: `200 OK`
- 대표 오류: `PROPERTY_MEMO_INVALID`, `AMBIGUOUS_MEMO_CONTENT`, `PROPERTY_NOT_FOUND`
- 메모 버전과 `expectedVersion`을 사용하지 않으며 마지막으로 DB에 반영된 요청이 최종값이다.
- v1.1 요청은 아래 여덟 필드를 모두 보내는 전체 교체다. 값은 빈 문자열일 수 있지만 누락과 `null`은 허용하지 않는다.
- 일곱 구조화 필드는 각각 200 유니코드 코드포인트 이하, `additionalMemo`는 5,000 코드포인트 이하다.

```json
{
  "viewingSchedule": "8월 20일 오후 2시 방문",
  "moveInAvailability": "9월 1일부터 입주 가능",
  "provisionalDeposit": "가계약금 30만 원",
  "roomOptions": "냉장고와 세탁기 포함",
  "maintenanceAndUtilities": "관리비와 전기·가스 별도",
  "commuteTime": "학교까지 버스로 20분",
  "governmentSupport": "중소기업 청년 대출 가능 여부 확인",
  "additionalMemo": "채광과 골목 소음을 다시 확인"
}
```

```json
{
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "data": {
    "viewingSchedule": "8월 20일 오후 2시 방문",
    "moveInAvailability": "9월 1일부터 입주 가능",
    "provisionalDeposit": "가계약금 30만 원",
    "roomOptions": "냉장고와 세탁기 포함",
    "maintenanceAndUtilities": "관리비와 전기·가스 별도",
    "commuteTime": "학교까지 버스로 20분",
    "governmentSupport": "중소기업 청년 대출 가능 여부 확인",
    "additionalMemo": "채광은 좋고 골목 소음은 다시 확인",
    "content": "채광은 좋고 골목 소음은 다시 확인",
    "savedAt": "2026-08-10T07:40:00Z"
  }
}
```

v1.0 클라이언트는 `{ "content": "..." }` 단독 요청을 계속 사용할 수 있다. 이 요청은 `additionalMemo`만 교체하고 기존 일곱 구조화 필드를 보존한다. `content`와 구조화 필드를 하나라도 함께 보내면 값이 `null`이어도 `AMBIGUOUS_MEMO_CONTENT`로 거부한다. 빈 요청, 불완전한 v1.1 요청, `null`, 길이 초과는 `PROPERTY_MEMO_INVALID`로 거부한다. 거부한 메모 원문은 오류 응답과 로그에 남기지 않는다.

저장은 소유 매물 행을 잠근 뒤 `property_pre_visit_memos` 전체 upsert와 `properties.memo`·`memo_updated_at` 호환 갱신을 한 트랜잭션에서 처리한다. 어느 한 쓰기라도 실패하면 두 저장소를 모두 롤백하며 저장 시각과 매물 활동 시각을 같은 DB 정밀도의 값으로 갱신한다.

## 매물 사진 규칙

- 사진은 체크리스트나 방문이 아니라 매물에 속하며 모든 접근에서 JWT 회원 ID와 매물 ID를 함께 검증한다.
- 업로드는 `file` 파트 한 개를 받는 단건 `multipart/form-data` 요청이다.
- JPEG(`image/jpeg`), PNG(`image/png`), WebP(`image/webp`)만 허용한다.
- 파일당 최대 10 MiB, 매물당 최대 30장이다.
- 선언 MIME, 바이트 시그니처와 실제 이미지 디코딩 가능 여부를 모두 검증한다.
- 목록은 `createdAt ASC, photoId ASC`인 업로드 순으로 정렬한다.
- 대표 사진 직접 선택, 순서 변경, 리사이징, 별도 썸네일, Presigned URL과 공개 URL은 제공하지 않는다.
- 원본 파일명은 저장하지 않는다. 내부 `storageKey`는 비공개 객체 접근용으로 DB에만 저장하고 API 응답과 로그에는 노출하지 않는다.

## API-201 매물 사진 목록 조회

- Method: `GET`
- URL: `/api/properties/{propertyId}/photos`
- 인증: 필요
- 성공 상태: `200 OK`
- 대표 오류: `PROPERTY_NOT_FOUND`

```json
{
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "data": {
    "photos": [
      {
        "photoId": 81,
        "contentUrl": "/api/properties/10/photos/81/content",
        "contentType": "image/jpeg",
        "sizeBytes": 245760,
        "createdAt": "2026-08-10T07:35:00Z"
      }
    ],
    "totalCount": 1
  }
}
```

## API-202 매물 사진 등록

- Method: `POST`
- URL: `/api/properties/{propertyId}/photos`
- Content-Type: `multipart/form-data`
- 파트: `file` 한 개
- 인증: 필요
- 성공 상태: `201 Created`
- `Location`: `/api/properties/{propertyId}/photos/{photoId}/content`
- 대표 오류: `PROPERTY_NOT_FOUND`, `PHOTO_FORMAT_UNSUPPORTED`, `PHOTO_SIZE_EXCEEDED`, `PHOTO_COUNT_EXCEEDED`, `PHOTO_UPLOAD_FAILED`
- 성공 응답의 `data`는 API-201의 사진 항목과 같다.

외부 객체를 먼저 비공개 저장한 뒤 매물 행을 잠그고 30장 제한을 다시 확인해 DB 메타데이터와 최근 활동을 저장한다. DB 단계가 실패하면 방금 저장한 객체를 동기 보상 삭제한다.

## API-203 매물 사진 본문 조회

- Method: `GET`
- URL: `/api/properties/{propertyId}/photos/{photoId}/content`
- 인증: 필요
- 성공 상태: `200 OK`
- 대표 오류: `PROPERTY_NOT_FOUND`, `PHOTO_NOT_FOUND`, `PHOTO_READ_FAILED`
- 성공 응답은 JSON 공통 래퍼 없이 저장된 원본 이미지 바이트를 스트리밍한다.
- 저장한 `Content-Type`, `Content-Length`와 `Cache-Control: private, no-store`를 응답한다.
- 백엔드가 소유권을 확인한 뒤 비공개 객체를 열며 저장소 URL과 내부 키를 노출하지 않는다.

## API-204 매물 사진 삭제

- Method: `DELETE`
- URL: `/api/properties/{propertyId}/photos/{photoId}`
- 인증: 필요
- 성공 상태: `204 No Content`
- 대표 오류: `PROPERTY_NOT_FOUND`, `PHOTO_NOT_FOUND`, `PHOTO_DELETE_FAILED`
- 외부 객체 삭제가 성공하거나 이미 없는 경우에만 DB 메타데이터를 삭제한다.
- DB 삭제가 실패하면 오류를 반환한다. 재요청에서는 이미 없는 외부 객체를 성공으로 보고 메타데이터 삭제를 다시 시도할 수 있다.

## 체크리스트 공통 규칙

- 단계는 `ONLINE_PHONE`, `ON_SITE`, `PRE_CONTRACT` 중 하나다.
- 저장 데이터와 v1.0 호환 입력의 프리셋 주거 유형은 `ONE_ROOM`, `GOSHIWON`을 유지한다. 신규 프론트엔드는 `ONE_ROOM`만 요청한다.
- 체크리스트 이름은 앞뒤 공백 제거 후 1자 이상 50자 이하다.
- 체크리스트에는 같은 단계의 `PROVIDED`와 로컬 `CUSTOM`을 합쳐 하나 이상의 항목을 둔다. 같은 제공 항목은 중복할 수 없지만 같은 CUSTOM 문구는 의도적으로 여러 번 둘 수 있다.
- `PROVIDED`는 전역 `check_items`의 활성 제공 항목을 가리키고, `CUSTOM`은 정확히 한 체크리스트의 `custom_question`에만 저장한다. CUSTOM은 API-301 제공 항목 검색과 API-302 프리셋에 나타나지 않는다.
- CUSTOM 질문은 앞뒤 공백 제거 후 1~200 유니코드 코드포인트다. 앞뒤 CR·LF도 공백으로 제거하되 trim 뒤 남는 내부 개행은 허용한다.
- v1.1 `items` 배열의 순서를 1부터 시작하는 `order`로 저장한다. v1.0 `checkItemIds`는 같은 순서의 PROVIDED 명령으로 변환한다.
- 저장된 각 항목은 제공·사용자 구분과 관계없이 안정적인 로컬 `checklistItemId`를 가진다. `sourceCheckItemId`와 deprecated `checkItemId`는 PROVIDED만 값이 있고 CUSTOM은 `null`이다.
- 회원은 같은 단계와 같은 이름의 체크리스트를 여러 개 만들 수 있다.
- 존재하지 않거나 다른 회원이 소유한 체크리스트는 모두 `CHECKLIST_NOT_FOUND`로 응답한다.
- `PUT`은 이름과 전체 항목 배열을 교체하며 단계는 바꾸지 않는다. 기존 항목은 `checklistItemId`를 보존하는 diff로 갱신하고 새 항목에만 새 ID를 발급한다. 같은 체크리스트의 동시 변경은 루트 행 잠금 후 커밋된 마지막 요청이 최종값이다.
- 이미 선택한 기준 항목은 이후 비활성화돼도 유지·재정렬할 수 있지만 새로 선택할 수 없다.
- `assignedPropertyCount`는 해당 체크리스트를 현재 활성으로 사용하는 서로 다른 매물 연결 수다.
- 과거 방문 스냅샷은 `assignedPropertyCount`에 포함하지 않는다.

## API-301 제공 체크 항목 검색

- Method: `GET`
- URL: `/api/check-items?stage=ON_SITE&query=보일러&page=0&size=20`
- 인증: 필요
- 성공 상태: `200 OK`
- 대표 오류: `INVALID_STAGE`, `INVALID_REQUEST`, `INVALID_PAGE_REQUEST`
- `stage`는 필수다.
- `query`는 선택이며 trim 후 0~500자 질문 부분 일치 검색이다. `%`, `_`, `!`는 SQL 패턴 문자가 아닌 일반 문자로 취급한다.
- 활성 항목만 `checkItemId ASC` 순으로 조회한다.
- `page` 기본값은 0, `size` 기본값은 20이며 1~100만 허용한다.

```json
{
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "data": {
    "content": [
      {
        "checkItemId": 101,
        "stage": "ON_SITE",
        "question": "보일러 상태는 괜찮은가?",
        "guide": "온수와 난방을 직접 작동해 확인합니다."
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "hasNext": false
  }
}
```

## API-302 체크리스트 프리셋 조회

- Method: `GET`
- URL: `/api/checklist-presets?presetType=ONE_ROOM&stage=ON_SITE`
- 인증: 필요
- 성공 상태: `200 OK`
- 대표 오류: `INVALID_STAGE`, `CHECKLIST_PRESET_NOT_FOUND`
- `presetType`과 `stage`는 필수다.
- 활성 프리셋과 활성 기준 항목만 정해진 `order ASC` 순으로 반환한다.
- v1.1에서는 `ONE_ROOM`만 활성 상태다. 보존된 `GOSHIWON` 값을 요청하면 `CHECKLIST_PRESET_NOT_FOUND`로 응답한다.
- 프리셋은 사용자 체크리스트를 직접 만들지 않는 읽기 전용 시작 데이터다.
- 프리셋에는 전역 PROVIDED 항목만 포함하고 회원의 CUSTOM 항목은 포함하지 않는다.

```json
{
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "data": {
    "presetType": "ONE_ROOM",
    "stage": "ON_SITE",
    "items": [
      {
        "checkItemId": 101,
        "question": "보일러 상태는 괜찮은가?",
        "guide": "온수와 난방을 직접 작동해 확인합니다.",
        "order": 1
      }
    ]
  }
}
```

## API-303 내 체크리스트 목록 조회

- Method: `GET`
- URL: `/api/checklists?stage=ON_SITE&page=0&size=20`
- 인증: 필요
- 성공 상태: `200 OK`
- 대표 오류: `INVALID_STAGE`, `INVALID_PAGE_REQUEST`
- `stage`는 필수다.
- `updatedAt DESC, checklistId DESC` 순으로 정렬한다.
- 결과가 없으면 `content: []`인 정상 응답을 반환한다.
- `itemCount`는 PROVIDED와 CUSTOM을 합한 실제 로컬 항목 수다.

```json
{
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "data": {
    "content": [
      {
        "checklistId": 31,
        "name": "현장 최종 확인",
        "stage": "ON_SITE",
        "itemCount": 2,
        "assignedPropertyCount": 2,
        "updatedAt": "2026-08-10T08:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "hasNext": false
  }
}
```

## API-304 내 체크리스트 생성

- Method: `POST`
- URL: `/api/checklists`
- 인증: 필요
- 성공 상태: `201 Created`
- `Location`: `/api/checklists/{checklistId}`
- 대표 오류: `INVALID_REQUEST`, `INVALID_STAGE`, `CHECKLIST_EMPTY`, `CHECKLIST_ITEM_DUPLICATED`, `CHECKLIST_ITEM_STAGE_MISMATCH`, `CUSTOM_CHECKLIST_ITEM_INVALID`, `CHECKLIST_ITEMS_REPRESENTATION_CONFLICT`, `CHECK_ITEM_INACTIVE`, `CHECK_ITEM_NOT_FOUND`
- 루트와 정렬된 항목을 하나의 트랜잭션으로 저장한다.
- v1.1은 `items`를 사용한다. PROVIDED는 `origin`, `sourceCheckItemId`만 보내고 CUSTOM은 `origin`, `question`만 보내며 생성 요청에 `checklistItemId`를 보낼 수 없다.
- v1.0 `checkItemIds` 단독 요청은 계속 허용하고 모두 PROVIDED로 해석한다. 두 표현을 함께 보내면 값이 `null`이어도 `CHECKLIST_ITEMS_REPRESENTATION_CONFLICT`로 거부한다.

```json
{
  "name": "현장 최종 확인",
  "stage": "ON_SITE",
  "items": [
    {
      "origin": "PROVIDED",
      "sourceCheckItemId": 101
    },
    {
      "origin": "CUSTOM",
      "question": "창틀 곰팡이는 괜찮은가?"
    }
  ]
}
```

```json
{
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "data": {
    "checklistId": 31,
    "name": "현장 최종 확인",
    "stage": "ON_SITE",
    "items": [
      {
        "checklistItemId": 701,
        "origin": "PROVIDED",
        "sourceCheckItemId": 101,
        "checkItemId": 101,
        "question": "보일러 상태는 괜찮은가?",
        "guide": "온수와 난방을 직접 작동해 확인합니다.",
        "order": 1
      },
      {
        "checklistItemId": 702,
        "origin": "CUSTOM",
        "sourceCheckItemId": null,
        "checkItemId": null,
        "question": "창틀 곰팡이는 괜찮은가?",
        "guide": null,
        "order": 2
      }
    ],
    "itemCount": 2,
    "assignedPropertyCount": 0,
    "createdAt": "2026-08-10T08:00:00Z",
    "updatedAt": "2026-08-10T08:00:00Z"
  }
}
```

## API-305 내 체크리스트 상세 조회

- Method: `GET`
- URL: `/api/checklists/{checklistId}`
- 인증: 필요
- 성공 상태: `200 OK`
- 대표 오류: `CHECKLIST_NOT_FOUND`
- 응답의 `items`는 저장한 순서를 유지한다. PROVIDED 질문·안내는 현재 `check_items` 값을, CUSTOM 질문은 체크리스트 로컬 값을 사용하며 CUSTOM 안내는 `null`이다.

```json
{
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "data": {
    "checklistId": 31,
    "name": "현장 최종 확인",
    "stage": "ON_SITE",
    "items": [
      {
        "checklistItemId": 701,
        "origin": "PROVIDED",
        "sourceCheckItemId": 101,
        "checkItemId": 101,
        "question": "보일러 상태는 괜찮은가?",
        "guide": "온수와 난방을 직접 작동해 확인합니다.",
        "order": 1
      },
      {
        "checklistItemId": 702,
        "origin": "CUSTOM",
        "sourceCheckItemId": null,
        "checkItemId": null,
        "question": "창틀 곰팡이는 괜찮은가?",
        "guide": null,
        "order": 2
      }
    ],
    "itemCount": 2,
    "assignedPropertyCount": 2,
    "createdAt": "2026-08-10T08:00:00Z",
    "updatedAt": "2026-08-10T08:00:00Z"
  }
}
```

## API-306 내 체크리스트 전체 변경

- Method: `PUT`
- URL: `/api/checklists/{checklistId}`
- 인증: 필요
- 성공 상태: `200 OK`
- 대표 오류: `INVALID_REQUEST`, `CHECKLIST_NOT_FOUND`, `CHECKLIST_ITEM_NOT_FOUND`, `CHECKLIST_EMPTY`, `CHECKLIST_ITEM_DUPLICATED`, `CHECKLIST_ITEM_STAGE_MISMATCH`, `CUSTOM_CHECKLIST_ITEM_INVALID`, `CHECKLIST_ITEMS_REPRESENTATION_CONFLICT`, `CHECKLIST_REQUIRES_V11_CLIENT`, `CHECK_ITEM_INACTIVE`, `CHECK_ITEM_NOT_FOUND`
- 요청에는 `stage`를 받지 않는다. 생성 시 단계를 유지하면서 이름과 전체 항목 배열을 원자적으로 교체한다. 현재 체크리스트 루트를 회원 조건으로 `FOR UPDATE` 잠근 뒤 검증·diff·루트 저장을 한 트랜잭션에서 처리한다.
- v1.1은 기존 CUSTOM을 `checklistItemId`로 유지·수정하고 식별자 없는 CUSTOM을 새로 만든다. PROVIDED는 `sourceCheckItemId`로 기존 로컬 항목을 찾아 ID를 보존한다. 최종 배열에서 빠진 항목만 삭제한다.
- 다른 체크리스트의 `checklistItemId`, PROVIDED 로컬 ID를 CUSTOM처럼 사용하는 요청과 존재하지 않는 로컬 ID는 소유권을 드러내지 않고 `CHECKLIST_ITEM_NOT_FOUND`로 응답한다.
- v1.0 `checkItemIds` 단독 요청은 기존 CUSTOM이 없을 때만 허용한다. CUSTOM이 있으면 이름과 PROVIDED 항목도 바꾸기 전에 `CHECKLIST_REQUIRES_V11_CLIENT` 409로 거부한다.
- `items`와 `checkItemIds`를 함께 보내면 값이 `null`이어도 `CHECKLIST_ITEMS_REPRESENTATION_CONFLICT` 400으로 거부한다.
- 성공 응답은 API-305의 상세 응답과 같다.

```json
{
  "name": "현장 재확인",
  "items": [
    {
      "origin": "CUSTOM",
      "checklistItemId": 702,
      "question": "곰팡이 냄새는 괜찮은가?"
    },
    {
      "origin": "PROVIDED",
      "sourceCheckItemId": 101
    },
    {
      "origin": "CUSTOM",
      "question": "환기 상태는 괜찮은가?"
    }
  ]
}
```

## API-307 내 체크리스트 삭제

- Method: `DELETE`
- URL: `/api/checklists/{checklistId}`
- 인증: 필요
- 성공 상태: `204 No Content`
- 대표 오류: `CHECKLIST_NOT_FOUND`
- 응답 본문 없이 체크리스트를 물리 삭제한다. PROVIDED·CUSTOM 로컬 항목과 현재 매물 활성 연결은 FK cascade로 함께 삭제하고 제공 기준 항목·프리셋·매물은 유지한다. 과거 방문은 질문·안내·순서·origin을 보존하고 출처 ID만 `null`이 될 수 있다.
- 체크리스트 수정은 현재 활성 연결이 참조하는 이름과 항목 수에 즉시 반영된다. 기존 방문 스냅샷은 변경하지 않는다.

## 활성 체크리스트 공통 규칙

- 매물과 체크리스트는 모두 JWT 인증 회원이 소유해야 하며 다른 회원 자원은 각각 `PROPERTY_NOT_FOUND`, `CHECKLIST_NOT_FOUND`로 숨긴다.
- 매물의 각 단계에는 활성 체크리스트를 최대 하나 연결하고 같은 체크리스트는 여러 매물에서 재사용할 수 있다.
- URL 단계와 체크리스트의 고정 단계가 다르면 `CHECKLIST_STAGE_MISMATCH`를 반환한다.
- 연결은 체크리스트 이름과 항목을 복제하지 않는 live 참조다. 체크리스트 수정은 현재 매물 상세에 반영되며 새 방문 시작 때만 별도 스냅샷한다.
- 설정·교체·해제는 매물 행을 `FOR UPDATE`로 잠가 같은 매물의 요청과 삭제를 직렬화한다. 동시에 같은 매물·단계에 서로 다른 체크리스트를 설정하면 잠금을 마지막으로 획득해 커밋한 요청 하나가 남는다.
- 매물 또는 체크리스트를 삭제하면 FK cascade로 현재 연결만 제거하고 반대편 루트는 삭제하지 않는다.
- 연결 변경 때만 매물의 `lastActivityAt`을 갱신한다. 이미 같은 체크리스트를 다시 설정하거나 없는 연결을 다시 해제하는 요청은 성공하되 활동 시각을 바꾸지 않는다.

## API-401 활성 체크리스트 설정·교체

- Method: `PUT`
- URL: `/api/properties/{propertyId}/active-checklists/{stage}`
- 인증: 필요
- 성공 상태: `200 OK`
- 대표 오류: `INVALID_REQUEST`, `INVALID_STAGE`, `PROPERTY_NOT_FOUND`, `CHECKLIST_NOT_FOUND`, `CHECKLIST_STAGE_MISMATCH`
- 연결이 없으면 새로 설정하고 이미 다른 체크리스트가 있으면 원자적으로 교체한다. 같은 체크리스트 재설정은 같은 응답을 반환하는 멱등 요청이다.

```json
{
  "checklistId": 7
}
```

```json
{
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "data": {
    "propertyId": 10,
    "stage": "ON_SITE",
    "checklistId": 7,
    "name": "나의 원룸 체크리스트",
    "itemCount": 22
  }
}
```

## API-402 활성 체크리스트 연결 해제

- Method: `DELETE`
- URL: `/api/properties/{propertyId}/active-checklists/{stage}`
- 인증: 필요
- 성공 상태: `204 No Content`
- 대표 오류: `INVALID_STAGE`, `PROPERTY_NOT_FOUND`
- 해당 단계의 연결만 삭제하고 체크리스트와 다른 매물·단계 연결은 유지한다.
- 이미 연결이 없어도 `204 No Content`로 처리한다.

## 방문 공통 규칙

- 방문 시작은 매물의 모든 활성 체크리스트 1~3개에서 PROVIDED와 CUSTOM을 이름·단계·origin·출처·질문·안내·순서와 함께 독립 스냅샷한다. 원본 변경·교체·해제·삭제와 기준 항목 비활성화는 기존 방문을 바꾸지 않는다.
- 체크 상태는 `GOOD`, `CAUTION`, `UNCONFIRMED`이며 모든 항목은 `UNCONFIRMED`, `statusVersion` 0, 빈 인라인 메모, `memoVersion` 0으로 시작한다. 물리 `version`은 논리 `statusVersion`이다.
- 상태와 메모는 값·버전·저장 시각을 독립 채널로 저장한다. API-504는 `expectedStatusVersion`, API-506은 `expectedMemoVersion`을 자기 채널에만 비교하며 같은 채널 충돌은 성공으로 응답하지 않는다.
- 미확인 항목이 있어도 완료할 수 있다. 완료 재요청은 최초 `completedAt`을 유지하고 완료 취소는 허용하지 않는다.
- 완료 후에도 항목을 수정할 수 있으며 완료 상태와 최초 완료 시각은 유지한다.
- 방문 시작·항목 저장·최초 완료는 매물 `lastActivityAt`을 갱신한다. 최근 방문 자체는 마지막 수정이 아니라 `startedAt DESC, visitId DESC`로 정한다.
- 다른 회원 자원은 `PROPERTY_NOT_FOUND`, `VISIT_NOT_FOUND`, `VISIT_ITEM_NOT_FOUND`로 존재를 숨긴다.

방문 상세와 API-502 응답은 다음 구조를 사용한다.

```json
{
  "visitId": 31,
  "propertyId": 10,
  "status": "IN_PROGRESS",
  "startedAt": "2026-08-09T05:20:00Z",
  "completedAt": null,
  "updatedAt": "2026-08-09T05:20:00Z",
  "stages": [
    {
      "stage": "ON_SITE",
      "sourceChecklistId": 7,
      "checklistName": "나의 원룸 체크리스트",
      "items": [
        {
          "visitItemId": 501,
          "origin": "PROVIDED",
          "sourceChecklistItemId": 701,
          "sourceCheckItemId": 101,
          "question": "보일러 상태는 괜찮은가?",
          "guide": "온수와 난방을 직접 작동해 확인합니다.",
          "order": 1,
          "status": "UNCONFIRMED",
          "statusVersion": 0,
          "statusSavedAt": "2026-08-09T05:20:00Z",
          "inlineMemo": "",
          "memoVersion": 0,
          "memoSavedAt": null,
          "version": 0,
          "savedAt": "2026-08-09T05:20:00Z"
        },
        {
          "visitItemId": 502,
          "origin": "CUSTOM",
          "sourceChecklistItemId": 702,
          "sourceCheckItemId": null,
          "question": "창틀 곰팡이는 괜찮은가?",
          "guide": null,
          "order": 2,
          "status": "UNCONFIRMED",
          "statusVersion": 0,
          "statusSavedAt": "2026-08-09T05:20:00Z",
          "inlineMemo": "",
          "memoVersion": 0,
          "memoSavedAt": null,
          "version": 0,
          "savedAt": "2026-08-09T05:20:00Z"
        }
      ],
      "summary": {
        "totalCount": 2,
        "checkedCount": 0,
        "goodCount": 0,
        "cautionCount": 0,
        "unconfirmedCount": 2
      }
    }
  ],
  "summary": {
    "totalCount": 2,
    "checkedCount": 0,
    "goodCount": 0,
    "cautionCount": 0,
    "unconfirmedCount": 2
  }
}
```

원본 체크리스트 삭제 후 `sourceChecklistId`는 `null`일 수 있다. 원본 로컬 항목을 삭제하면 `sourceChecklistItemId`만 `null`이 되며 origin·질문·안내·순서·상태·메모와 두 version은 유지한다. `sourceCheckItemId`는 PROVIDED만 값이 있고 CUSTOM은 항상 `null`이다. `memoSavedAt`은 최초 메모 저장 전 `null`이며 `version`, `savedAt`은 각각 `statusVersion`, `statusSavedAt`과 같은 deprecated v1.0 별칭이다.

## API-501 방문 기록 목록 조회

- Method: `GET`
- URL: `/api/properties/{propertyId}/visits?page=0&size=20`
- 인증: 필요
- 성공 상태: `200 OK`
- 대표 오류: `PROPERTY_NOT_FOUND`, `INVALID_PAGE_REQUEST`
- `startedAt DESC, visitId DESC` 순으로 정렬하고 방문별 상태·시각·전체 집계를 한 조회로 반환한다. 방문이 없으면 빈 페이지다.

## API-502 새 방문 시작

- Method: `POST`
- URL: `/api/properties/{propertyId}/visits`
- Body: 없음
- 인증: 필요
- 성공 상태: `201 Created`
- `Location`: `/api/visits/{visitId}`
- 대표 오류: `PROPERTY_NOT_FOUND`, `ACTIVE_CHECKLIST_REQUIRED`, `CHECKLIST_SNAPSHOT_FAILED`
- 매물과 체크리스트 루트를 순서대로 잠그고 방문·PROVIDED·CUSTOM 전체 스냅샷 항목·매물 활동 시각을 하나의 트랜잭션에서 저장한다. CUSTOM은 당시 로컬 질문과 `null` 안내를 복사하고 제공 카탈로그를 만들거나 갱신하지 않는다.

## API-503 방문 기록 상세 조회

- Method: `GET`
- URL: `/api/visits/{visitId}`
- 인증: 필요
- 성공 상태: `200 OK`
- 대표 오류: `VISIT_NOT_FOUND`
- 진행·완료 방문 모두 공통 상세 구조를 반환하며 단계와 항목은 고정 단계 순·스냅샷 순서로 정렬한다. 항목마다 origin, nullable 출처, 질문 스냅샷과 `status`·`statusVersion`·`statusSavedAt`, `inlineMemo`·`memoVersion`·nullable `memoSavedAt`을 반환한다.
- `version=statusVersion`, `savedAt=statusSavedAt`인 deprecated v1.0 필드를 함께 반환한다. backfill 또는 부분 배포 행의 `status_saved_at`이 `NULL`이면 조회에서 항목 `updated_at`으로 fallback한다.

## API-504 확인 상태 자동 저장

- Method: `PATCH`
- URL: `/api/visits/{visitId}/items/{visitItemId}`
- 인증: 필요
- 성공 상태: `200 OK`
- 대표 오류: `VISIT_NOT_FOUND`, `VISIT_ITEM_NOT_FOUND`, `INVALID_CHECK_STATUS`, `AMBIGUOUS_STATUS_VERSION`, `VISIT_ITEM_STATUS_VERSION_CONFLICT`

```json
{
  "status": "CAUTION",
  "expectedStatusVersion": 0
}
```

v1.0의 `expectedVersion`은 `expectedStatusVersion` 별칭으로 계속 읽는다. 두 필드를 함께 보내면 같은 값이어야 하며 다르면 `AMBIGUOUS_STATUS_VERSION` 400이다. 누락·명시적 `null`·음수는 `INVALID_REQUEST` 400이다.

성공 응답은 변경 항목의 ID·상태·증가한 `statusVersion`·`statusSavedAt`, 같은 값의 deprecated `version`·`savedAt`과 기존 해당 단계·방문 전체 집계를 반환한다. 상태 UPDATE는 메모 값·버전·저장 시각을 읽거나 변경하지 않는다. 현재 상태 version과 다르면 `VISIT_ITEM_STATUS_VERSION_CONFLICT` 409다.

## API-505 방문 완료

- Method: `PATCH`
- URL: `/api/visits/{visitId}`
- 인증: 필요
- 성공 상태: `200 OK`
- 대표 오류: `VISIT_NOT_FOUND`, `INVALID_VISIT_STATUS`

```json
{
  "status": "COMPLETED"
}
```

성공 응답은 방문 ID·상태·시작·완료 시각과 전체 집계를 반환한다.

## API-506 인라인 메모 자동 저장

- Method: `PATCH`
- URL: `/api/visits/{visitId}/items/{visitItemId}/memo`
- 인증: 필요
- 성공 상태: `200 OK`
- 대표 오류: `VISIT_NOT_FOUND`, `VISIT_ITEM_NOT_FOUND`, `VISIT_ITEM_MEMO_INVALID`, `VISIT_ITEM_MEMO_VERSION_CONFLICT`, `INVALID_REQUEST`

```json
{
  "memo": "창틀 주변 습기를 다시 확인",
  "expectedMemoVersion": 0
}
```

`memo`는 `null`이 아닌 최대 200 Unicode 코드포인트 한 줄 문자열이며 CR·LF를 허용하지 않는다. 앞뒤 공백은 그대로 보존하고 빈 문자열로 지울 수 있다. 같은 값을 다시 저장해도 version이 일치하면 명시적 저장으로 처리해 `memoVersion`을 1 증가시키고 `memoSavedAt`을 갱신한다.

성공 응답은 `visitItemId`, `memo`, 증가한 `memoVersion`, `memoSavedAt`만 반환한다. 메모 UPDATE는 상태 값·버전·저장 시각과 집계를 읽거나 변경하지 않는다. 현재 메모 version과 다르면 `VISIT_ITEM_MEMO_VERSION_CONFLICT` 409다. 완료 방문에서도 저장할 수 있으며 방문 완료 상태와 최초 `completedAt`은 유지한다.

## 프론트엔드 v1.1 인계 기준선

프론트엔드는 Notion 목표 설계를 다시 해석하지 않고 이 문서와 실행 중인 OpenAPI를 함께 사용한다. 백엔드 기준선 버전은 OpenAPI `info.version`의 `1차 MVP v1.1`이며 비즈니스 연산은 [구현 추적](#구현-추적)의 API-001~506 정확히 27개다. API-001만 공개하고 나머지 26개는 Bearer 인증을 요구한다. 실행 환경에서 `/v3/api-docs`가 이 기준과 다르면 배포된 백엔드와 문서 버전 불일치로 처리한다.

### 전송·인증·공통 응답

- Access Token은 브라우저 영구 저장소가 아닌 애플리케이션 메모리에만 보관하고 모든 보호 API에 `Authorization: Bearer {accessToken}`을 보낸다. Refresh·Session·Logout API는 없다.
- 인증 회원 ID는 Controller의 `@AuthenticatedMemberId`로 JWT subject에서만 주입하며 Body·Query·Path 입력으로 받지 않는다. OpenAPI parameter에도 내부 `memberId`를 노출하지 않는다. 요청에 같은 이름의 임의 query를 추가해도 인증 주체는 바뀌지 않는다.
- JSON 성공 응답은 `code`, `message`, `data` envelope를 사용하고 오류 응답은 `code`, `message`, `errors`를 사용한다. HTTP 상태와 `code`를 함께 분기하며 오류 메시지 문자열을 프로그램 조건으로 사용하지 않는다.
- 페이지 응답은 `content`, 0부터 시작하는 `page`, `size`, `totalElements`, `totalPages`, `hasNext`를 사용한다. 빈 결과는 오류가 아니라 `content: []`다.
- 모든 시각은 UTC `Instant`의 ISO-8601 문자열로 읽고 표시할 때만 사용자 시간대로 변환한다. DB는 마이크로초 정밀도를 사용하므로 문자열 정밀도를 고정 길이로 가정하지 않는다.
- `204 No Content`인 API-105·204·307·402는 JSON을 파싱하지 않는다.
- `null`은 원본 또는 아직 생성되지 않은 값이고 빈 문자열·빈 배열과 다르다. `recentVisit`, `completedAt`, `memoSavedAt`, 원본 삭제 뒤 source ID와 CUSTOM `guide`는 `null`일 수 있다. 메모의 빈 문자열은 저장된 빈 값이고 컬렉션의 빈 배열은 정상적인 0개 결과다.

주요 쓰기 입력의 길이와 nullable 계약은 다음과 같다. 각 API 절의 더 엄격한 조합 규칙을 함께 적용한다.

| 입력 | 길이·nullable | 빈 문자열 |
| --- | --- | --- |
| 매물 `name` | trim 후 1~50 코드포인트, `null` 불가 | 불가 |
| 매물 `discoverySource` | trim 후 1~500 코드포인트, `null` 불가 | 불가 |
| 일곱 구조화 사전 메모 필드 | 각각 0~200 코드포인트, 여덟 필드 전체 요청에서 `null`·누락 불가 | 지우기로 허용 |
| `additionalMemo`·legacy `content` | 0~5,000 코드포인트, 선택한 표현 안에서 `null` 불가 | 지우기로 허용 |
| 체크리스트 `name` | trim 후 1~50 코드포인트, `null` 불가 | 불가 |
| CUSTOM `question` | trim 후 1~200 코드포인트, `null` 불가 | 불가 |
| 방문 `inlineMemo` 요청의 `memo` | 0~200 코드포인트, `null`·CR·LF 불가 | 지우기로 허용 |

### 사전 메모·체크리스트·방문

- API-106 v1.1 요청은 일곱 구조화 필드와 `additionalMemo` 여덟 개를 모두 보내는 전체 교체다. legacy `content` 단독 요청은 기존 일곱 필드를 보존하고 추가 메모만 변경한다. 두 표현을 섞지 않으며 이 API에는 version·`expectedVersion`이 없다.
- API-304·306의 정본은 `items` 배열이다. PROVIDED는 `{origin, sourceCheckItemId}`, CUSTOM은 `{origin, question}`을 보내고, 변경 시 기존 CUSTOM을 유지하려면 응답에서 받은 `checklistItemId`를 함께 돌려보낸다. 로컬 ID를 빼면 새 CUSTOM으로 생성되고 교체에서 빠진 항목은 삭제된다.
- 방문 시작 시 체크리스트 이름·단계와 PROVIDED·CUSTOM의 origin·출처·질문·안내·순서를 복사한다. 이후 원본 수정·해제·삭제는 기존 방문 표시를 바꾸지 않으며 원본이 삭제된 source ID만 `null`이 된다.
- 상태와 메모는 독립 저장 채널이다. API-504에는 최신 `statusVersion`을 `expectedStatusVersion`으로 보내고 성공 응답의 증가한 버전을 반영한다. API-506에는 최신 `memoVersion`을 `expectedMemoVersion`으로 보내고 성공 응답의 증가한 버전을 반영한다. 한 채널의 저장 결과로 다른 채널의 값이나 version을 덮어쓰지 않는다.
- 방문 완료는 `PATCH /api/visits/{visitId}`에 `{ "status": "COMPLETED" }`를 보낸다. 미확인 항목이 있어도 완료할 수 있고 완료 뒤에도 상태와 메모를 저장할 수 있으며 최초 `completedAt`은 바뀌지 않는다.

### 자동 저장과 충돌 처리

- 인라인 메모 입력이 멈춘 뒤 1초 이내 API-506 저장을 시도한다. blur·화면 이동·방문 완료 전에는 debounce를 기다리지 않고 pending 메모를 즉시 저장 시도하며 결과를 확인한 뒤 다음 동작을 진행한다.
- 상태 API-504와 메모 API-506 요청 큐를 분리한다. 같은 `visitItemId`의 같은 채널 요청은 직렬화하고 응답받은 최신 version으로 다음 요청을 보낸다. 서로 다른 채널은 상대 version을 기다리거나 복사하지 않는다.
- `409`는 저장 성공이 아니다. 해당 방문 상세를 다시 조회해 서버 값과 최신 version을 반영하고, 사용자의 미반영 입력을 보존한 채 재적용 여부를 결정한다. 충돌 응답에서 현재 값이나 version을 얻을 수 있다고 가정하지 않는다.
- 백엔드는 조건부 UPDATE, 소유권 확인과 채널 독립성만 보장한다. 1초 debounce, pending 상태, 화면 이동 전 flush, 충돌 UI와 재적용 정책은 프론트엔드 책임이며 백엔드에 별도 자동 저장 큐나 idempotency API는 없다.

### 사진 접근

- 업로드는 `multipart/form-data`의 단일 `file` binary 파트를 사용한다. 사진 용도 필드나 공개 URL은 없다.
- `contentUrl`은 인증이 필요한 API-203 상대 경로다. Bearer 헤더가 있는 `fetch`로 Blob을 받은 뒤 object URL로 표시하고 사용이 끝나면 해제한다. HTML 이미지 태그가 Bearer 헤더 없이 경로를 직접 요청할 수 있다고 가정하지 않는다.
- 사진 응답은 공통 JSON envelope가 아닌 원본 바이트이며 응답 `Content-Type`, `Content-Length`, `Cache-Control: private, no-store`를 따른다.

### deprecated 호환 필드

| deprecated 필드 | 신규 프론트엔드 사용 방향 |
| --- | --- |
| 사전 메모 `content` | 읽기·쓰기 모두 `additionalMemo`와 여덟 필드 요청으로 전환한다. |
| 체크리스트 요청 `checkItemIds` | `items` 배열로 전환한다. |
| 체크리스트 응답 `checkItemId` | 로컬 식별은 `checklistItemId`, PROVIDED 전역 출처는 `sourceCheckItemId`를 사용한다. |
| 상태 요청 `expectedVersion` | `expectedStatusVersion`을 사용한다. |
| 방문 항목 응답 `version`·`savedAt` | `statusVersion`·`statusSavedAt`을 사용한다. |

호환 필드는 이 기준선에서 제거하지 않는다. 신규 프론트엔드는 정본 필드만 쓰되 legacy 응답 필드가 함께 오는 것을 오류로 취급하지 않는다.

## 오류 코드

| HTTP | code | 의미 |
| --- | --- | --- |
| 400 | `INVALID_REQUEST` | 필수값, 형식 또는 범위가 올바르지 않음 |
| 400 | `INVALID_PAGE_REQUEST` | page 또는 size가 허용 범위를 벗어남 |
| 400 | `PROPERTY_MEMO_INVALID` | 구조화 메모가 누락·null·길이 제한 위반이거나 legacy content가 유효하지 않음 |
| 400 | `AMBIGUOUS_MEMO_CONTENT` | legacy content와 v1.1 구조화 필드를 한 요청에 함께 사용함 |
| 400 | `GOOGLE_AUTHORIZATION_CODE_INVALID` | Google code, code verifier 또는 redirect URI가 유효하지 않음 |
| 400 | `GOOGLE_IDENTITY_INVALID` | ID Token의 서명·claim·nonce 또는 필수 프로필이 유효하지 않음 |
| 401 | `UNAUTHENTICATED` | Bearer Access Token이 없음 |
| 401 | `ACCESS_TOKEN_EXPIRED` | 자취선배 Access Token이 만료됨 |
| 401 | `ACCESS_TOKEN_INVALID` | JWT 서명, issuer, audience, subject 또는 형식이 올바르지 않음 |
| 404 | `PROPERTY_NOT_FOUND` | 매물이 없거나 인증 회원이 소유하지 않음 |
| 404 | `PHOTO_NOT_FOUND` | 사진이 없거나 요청한 소유 매물에 속하지 않음 |
| 400 | `PHOTO_FORMAT_UNSUPPORTED` | MIME·시그니처·디코딩 검증을 통과하지 못함 |
| 400 | `PHOTO_SIZE_EXCEEDED` | 사진이 10 MiB 또는 multipart 제한을 초과함 |
| 400 | `PHOTO_COUNT_EXCEEDED` | 매물의 사진이 이미 30장임 |
| 500 | `PHOTO_UPLOAD_FAILED` | 외부 객체 또는 DB 메타데이터 저장 실패 |
| 500 | `PHOTO_READ_FAILED` | 비공개 객체 읽기 실패 |
| 500 | `PHOTO_DELETE_FAILED` | 사진 또는 매물의 외부 객체·DB 삭제 실패 |
| 400 | `INVALID_STAGE` | 체크 단계가 없거나 허용 값이 아님 |
| 404 | `CHECK_ITEM_NOT_FOUND` | 요청한 제공 체크 항목이 없음 |
| 400 | `CHECK_ITEM_INACTIVE` | 비활성 제공 체크 항목을 새로 선택함 |
| 404 | `CHECKLIST_PRESET_NOT_FOUND` | 요청한 주거 유형·단계 프리셋이 없음 |
| 404 | `CHECKLIST_NOT_FOUND` | 체크리스트가 없거나 인증 회원이 소유하지 않음 |
| 400 | `CHECKLIST_EMPTY` | 체크리스트 항목이 비어 있음 |
| 400 | `CHECKLIST_ITEM_DUPLICATED` | 같은 제공 체크 항목이 중복됨 |
| 400 | `CHECKLIST_ITEM_STAGE_MISMATCH` | 체크리스트와 제공 체크 항목의 단계가 다름 |
| 400 | `CUSTOM_CHECKLIST_ITEM_INVALID` | origin·출처 조합 또는 trim 후 CUSTOM 질문 길이가 올바르지 않음 |
| 404 | `CHECKLIST_ITEM_NOT_FOUND` | 로컬 항목이 없거나 요청 체크리스트에 속하지 않음 |
| 400 | `CHECKLIST_ITEMS_REPRESENTATION_CONFLICT` | v1.1 items와 v1.0 checkItemIds를 함께 사용함 |
| 409 | `CHECKLIST_REQUIRES_V11_CLIENT` | CUSTOM이 있는 체크리스트를 v1.0 전체 교체로 변경하려 함 |
| 400 | `CHECKLIST_STAGE_MISMATCH` | 단계 고정 자원에 다른 단계 체크리스트를 사용함 |
| 404 | `VISIT_NOT_FOUND` | 방문이 없거나 인증 회원이 소유하지 않음 |
| 404 | `VISIT_ITEM_NOT_FOUND` | 항목이 없거나 요청 방문에 속하지 않음 |
| 400 | `ACTIVE_CHECKLIST_REQUIRED` | 방문을 시작할 활성 체크리스트가 없음 |
| 400 | `INVALID_CHECK_STATUS` | 확인 상태가 허용 값이 아님 |
| 400 | `INVALID_VISIT_STATUS` | 완료 외 방문 상태 변경을 요청함 |
| 400 | `VISIT_ITEM_MEMO_INVALID` | 인라인 메모가 null·200 코드포인트 초과 또는 CR·LF 포함임 |
| 400 | `AMBIGUOUS_STATUS_VERSION` | expectedStatusVersion과 expectedVersion을 다른 값으로 함께 보냄 |
| 409 | `VISIT_ITEM_STATUS_VERSION_CONFLICT` | expected 상태 version이 현재 상태 version과 다름 |
| 409 | `VISIT_ITEM_MEMO_VERSION_CONFLICT` | expected 메모 version이 현재 메모 version과 다름 |
| 500 | `CHECKLIST_SNAPSHOT_FAILED` | 방문 체크리스트 전체 스냅샷 생성 실패 |
| 500 | `INTERNAL_SERVER_ERROR` | 예상하지 못한 내부 오류 |
| 502 | `GOOGLE_AUTHENTICATION_FAILED` | Google 통신, JWK 조회 또는 상류 응답 처리 실패 |

## 비범위

- Refresh Token과 토큰 재발급 API
- 서버 세션과 Spring Session 테이블
- 서버 로그아웃과 Access Token 차단 목록
- 방문·항목별 사진과 방문 개별 삭제 API
- 이미지 리사이징·압축·EXIF 제거·썸네일·CDN·Presigned URL
- 비동기 파일 삭제 큐, Outbox와 재시도 스케줄러
- v1.0 `checkItemIds`, deprecated `checkItemId` 제거와 호환 분기 cleanup
- 프론트엔드 코드의 인라인 메모 1초 debounce, 화면 이동·완료 전 pending 저장 flush

API-506은 프론트엔드가 안전하게 자동 저장할 독립 CAS 경계만 제공한다. 호출 직렬화·debounce·이동 전 flush의 제품 계약은 이 문서에 확정했지만 구현은 후속 프론트 작업이다. deprecated `expectedVersion`·`version`·`savedAt` 제거는 v1.0 관찰 뒤 별도 legacy cleanup 작업이다.

## 구현 추적

각 API 절의 입력·검증·응답 nullable·정렬·검색·페이징·멱등성·오류·소유권 숨김 규칙을 구현과 대조했다. 아래 표는 이 저장소에서 유일한 27개 API 추적표이며 다른 문서는 이 절을 링크한다. 모든 행은 실제 `/v3/api-docs`에 노출되며 `MvpBackendBaselineAcceptanceTest`가 정확히 27개 연산, 공개 API-001과 보호 API 26개, Bearer·`401`·성공 상태, v1.1 핵심 스키마, 금지 API 부재를 일괄 검증한다.

| API | Method·path | 인증·성공 | 트랜잭션·관련 테이블 | 계층 책임 | 자동화 근거 |
| --- | --- | --- | --- | --- | --- |
| API-001 | `POST /api/auth/google` | 공개·`200` | Google 호출 밖 / 회원 저장 트랜잭션, `members` | `AuthController` → `GoogleLoginService` → `MemberAuthenticationService`·`MemberRepository` | `GoogleAuthenticationAcceptanceTest`, MVP 기준선 |
| API-002 | `GET /api/members/me` | Bearer·`200` | 읽기 전용, `members` | `MemberController` → `MemberQueryService` → `MemberRepository` | `GoogleAuthenticationAcceptanceTest`, MVP 기준선 |
| API-101 | `GET /api/properties` | Bearer·`200` | 읽기 전용, `properties`, `property_photos`, 방문 3개 테이블 | `PropertyController` → `PropertyQueryService` → `PropertyQueryRepository` | `PropertyAcceptanceTest`, `PropertyRepositoryTest`, MVP 기준선 |
| API-102 | `POST /api/properties` | Bearer·`201` | 매물 저장 트랜잭션, `properties` | `PropertyController` → `PropertyCommandService` → `PropertyRepository` | `PropertyAcceptanceTest`, MVP 기준선 |
| API-103 | `GET /api/properties/{propertyId}` | Bearer·`200` | 읽기 전용, `properties`, `property_pre_visit_memos`, 사진·활성 체크리스트·방문 테이블 | `PropertyController` → `PropertyQueryService` → 매물·사진·활성 체크리스트 Repository | `PropertyAcceptanceTest`, `PropertyPreVisitMemoRepositoryTest`, MVP 기준선 |
| API-104 | `PATCH /api/properties/{propertyId}` | Bearer·`200` | 소유 매물 행 잠금·변경 트랜잭션, `properties` | `PropertyController` → `PropertyCommandService` → `PropertyRepository` | `PropertyAcceptanceTest`, MVP 기준선 |
| API-105 | `DELETE /api/properties/{propertyId}` | Bearer·`204` | 객체 삭제 뒤 짧은 DB 트랜잭션, 매물과 사진·활성 연결·방문 cascade | `PropertyController` → `PropertyDeletionService` → `PropertyDeleteTransactionService`·매물/사진 Repository | `PropertyAcceptanceTest`, `PropertyPhotoServiceIntegrationTest`, MVP 기준선 |
| API-106 | `PUT /api/properties/{propertyId}/memo` | Bearer·`200` | 소유 매물 행 잠금·원자적 dual-write, `property_pre_visit_memos`, `properties` | `PropertyController` → `PropertyCommandService` → `PropertyPreVisitMemoRepository`·`PropertyRepository` | `PropertyAcceptanceTest`, `PropertyServiceIntegrationTest`, `PropertyMemoConcurrencyIntegrationTest` |
| API-201 | `GET /api/properties/{propertyId}/photos` | Bearer·`200` | 읽기, `properties`, `property_photos` | `PropertyPhotoController` → `PropertyPhotoService` → `PropertyPhotoRepository` | `PropertyPhotoAcceptanceTest`, `PropertyPhotoRepositoryTest`, MVP 기준선 |
| API-202 | `POST /api/properties/{propertyId}/photos` | Bearer·`201` | 객체 저장 뒤 매물 잠금·메타데이터 트랜잭션, `properties`, `property_photos` | `PropertyPhotoController` → `PropertyPhotoService` → `PhotoStorage`·`PropertyPhotoTransactionService` | `PropertyPhotoAcceptanceTest`, `PropertyPhotoServiceTest`, 동시성 테스트, MVP 기준선 |
| API-203 | `GET /api/properties/{propertyId}/photos/{photoId}/content` | Bearer·`200` | 소유권 DB 조회 뒤 비공개 객체 읽기, `properties`, `property_photos` | `PropertyPhotoController` → `PropertyPhotoService` → 사진 Repository·`PhotoStorage` | `PropertyPhotoAcceptanceTest`, MVP 기준선 |
| API-204 | `DELETE /api/properties/{propertyId}/photos/{photoId}` | Bearer·`204` | 객체 삭제 뒤 메타데이터 트랜잭션, `properties`, `property_photos` | `PropertyPhotoController` → `PropertyPhotoService` → `PhotoStorage`·`PropertyPhotoTransactionService` | `PropertyPhotoAcceptanceTest`, `PropertyPhotoServiceTest`, MVP 기준선 |
| API-301 | `GET /api/check-items` | Bearer·`200` | 읽기, `check_items` | `CheckItemController` → `CheckCatalogQueryService` → `CheckItemRepository` | `ChecklistAcceptanceTest`, `ChecklistRepositoryTest`, MVP 기준선 |
| API-302 | `GET /api/checklist-presets` | Bearer·`200` | 읽기, `checklist_presets`, `checklist_preset_items`, `check_items` | `ChecklistPresetController` → `CheckCatalogQueryService` → 프리셋 Repository | `ChecklistAcceptanceTest`, `ChecklistRepositoryTest`, MVP 기준선 |
| API-303 | `GET /api/checklists` | Bearer·`200` | 읽기 전용, `checklists`, `checklist_items`, `property_active_checklists` | `ChecklistController` → `ChecklistQueryService` → `ChecklistQueryRepository` | `ChecklistAcceptanceTest`, `ChecklistRepositoryTest`, MVP 기준선 |
| API-304 | `POST /api/checklists` | Bearer·`201` | PROVIDED·CUSTOM 루트·항목 저장 트랜잭션, `checklists`, `checklist_items`, `check_items` | `ChecklistController` → 표현 정규화 → `ChecklistCommandService` → 체크리스트·제공 항목 Repository | `ChecklistAcceptanceTest`, `ChecklistRequestTest`, 통합·Repository 테스트, MVP 기준선 |
| API-305 | `GET /api/checklists/{checklistId}` | Bearer·`200` | 읽기 전용, `checklists`, `checklist_items` LEFT JOIN `check_items`, 활성 연결 | `ChecklistController` → `ChecklistQueryService` → `ChecklistQueryRepository` | `ChecklistAcceptanceTest`, `ChecklistRepositoryTest`, MVP 기준선 |
| API-306 | `PUT /api/checklists/{checklistId}` | Bearer·`200`·legacy CUSTOM 보호 `409` | 체크리스트 잠금·ID 보존 diff 트랜잭션, 체크리스트 3개 테이블 | `ChecklistController` → 표현 정규화 → `ChecklistCommandService` → `ChecklistRepository` | `ChecklistAcceptanceTest`, ID 보존·409·동시성·롤백 통합 테스트, MVP 기준선 |
| API-307 | `DELETE /api/checklists/{checklistId}` | Bearer·`204` | 체크리스트 잠금·삭제 트랜잭션, 항목·활성 연결 cascade, 방문 원본 FK `SET NULL` | `ChecklistController` → `ChecklistCommandService` → `ChecklistRepository` | `ChecklistAcceptanceTest`, `VisitRepositoryTest`, MVP 기준선 |
| API-401 | `PUT /api/properties/{propertyId}/active-checklists/{stage}` | Bearer·`200` | 매물→체크리스트 잠금·제한 재시도, `properties`, `checklists`, `property_active_checklists` | `ActiveChecklistController` → `ActiveChecklistService` → 매물·체크리스트·활성 연결 Repository | `ActiveChecklistAcceptanceTest`, 동시성·Repository 테스트, MVP 기준선 |
| API-402 | `DELETE /api/properties/{propertyId}/active-checklists/{stage}` | Bearer·`204` | 매물 잠금·멱등 삭제·제한 재시도, `properties`, `property_active_checklists` | `ActiveChecklistController` → `ActiveChecklistService` → 매물·활성 연결 Repository | `ActiveChecklistAcceptanceTest`, 동시성 테스트, MVP 기준선 |
| API-501 | `GET /api/properties/{propertyId}/visits` | Bearer·`200` | 읽기 전용, 방문 3개 테이블 | `VisitController` → `VisitQueryService` → `VisitQueryRepository` | `VisitAcceptanceTest`, `VisitRepositoryTest`, MVP 기준선 |
| API-502 | `POST /api/properties/{propertyId}/visits` | Bearer·`201` | 매물→체크리스트 잠금·PROVIDED·CUSTOM 전체 스냅샷 단일 트랜잭션·제한 재시도, 매물·활성 체크리스트·방문 3개 테이블 | `VisitController` → `VisitCommandService`·`ChecklistSnapshotSourceService` → 방문 Repository | `VisitAcceptanceTest`, CUSTOM 불변 스냅샷 통합·동시성 테스트, MVP 기준선 |
| API-503 | `GET /api/visits/{visitId}` | Bearer·`200` | origin·nullable 출처와 독립 상태·메모 필드를 포함한 방문 3개 테이블 읽기 | `VisitController` → `VisitQueryService` → `VisitQueryRepository` | `VisitAcceptanceTest`, CUSTOM 원본 삭제·메모 보존 통합 테스트, MVP 기준선 |
| API-504 | `PATCH /api/visits/{visitId}/items/{visitItemId}` | Bearer·`200`·상태 충돌 `409` | 매물→방문 잠금·상태 version 조건부 UPDATE·제한 재시도, `properties`, 방문 3개 테이블 | `VisitController` → `VisitCommandService.updateItemStatus` → 방문·항목 Repository | 요청 DTO·Repository·통합·동시성·인수 테스트, MVP 기준선 |
| API-505 | `PATCH /api/visits/{visitId}` | Bearer·`200` | 매물→방문 잠금·최초 완료 트랜잭션·제한 재시도, `properties`, `visits` | `VisitController` → `VisitCommandService` → `VisitRepository`·`VisitQueryRepository` | `VisitAcceptanceTest`, 방문 통합·동시성 테스트, MVP 기준선 |
| API-506 | `PATCH /api/visits/{visitId}/items/{visitItemId}/memo` | Bearer·`200`·메모 충돌 `409` | 매물→방문 잠금·메모 version 조건부 UPDATE·제한 재시도, `properties`, 방문 3개 테이블 | `VisitController` → `VisitCommandService.updateItemMemo` → 방문·항목 Repository | 도메인·Repository·통합·동시성·인수 테스트, MVP 기준선 |

초기 구현 이력은 [#1](https://github.com/Jachwi-Sunbae-Playground/moca/issues/1), [#3](https://github.com/Jachwi-Sunbae-Playground/moca/issues/3), [#5](https://github.com/Jachwi-Sunbae-Playground/moca/issues/5), [#7](https://github.com/Jachwi-Sunbae-Playground/moca/issues/7), [#9](https://github.com/Jachwi-Sunbae-Playground/moca/issues/9), [#11](https://github.com/Jachwi-Sunbae-Playground/moca/issues/11), [#13](https://github.com/Jachwi-Sunbae-Playground/moca/issues/13)에 남아 있다. v1.1 구현은 [#25](https://github.com/Jachwi-Sunbae-Playground/moca/issues/25), [#27](https://github.com/Jachwi-Sunbae-Playground/moca/issues/27), [#29](https://github.com/Jachwi-Sunbae-Playground/moca/issues/29), [#31](https://github.com/Jachwi-Sunbae-Playground/moca/issues/31), 최종 감사는 [#33](https://github.com/Jachwi-Sunbae-Playground/moca/issues/33)에서 추적한다.
