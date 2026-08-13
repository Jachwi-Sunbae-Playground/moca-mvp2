# 데이터베이스 모델

- 상태: BE-v1.1-4 방문 항목 상태·인라인 메모 독립 CAS 구현
- 현재 모델: 회원·매물·사전 메모·매물 사진·체크 항목·프리셋·회원 체크리스트·매물 활성 연결·방문·방문 스냅샷 테이블

## 기본 규칙

- 테이블과 컬럼은 `snake_case`를 사용한다.
- 기본키는 `id`, 외래키는 `{referenced_table}_id`를 기본으로 한다.
- 생성·수정 시각이 필요한 테이블은 `created_at`, `updated_at`을 사용한다.
- 삭제는 물리 삭제를 기본으로 하고 복구나 이력 보존 요구가 확인될 때만 soft delete를 사용한다.
- 중복 방지처럼 데이터 정합성에 필요한 규칙은 애플리케이션 검증과 데이터베이스 제약조건을 함께 검토한다.

## 회원

### `members`

Google 계정과 내부 회원 식별자를 연결한다.

| 컬럼 | 타입 | NULL | 의미 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | N | 자동 증가 PK |
| `oauth_provider` | `VARCHAR(20)` | N | 현재 `GOOGLE`만 허용 |
| `oauth_subject` | `VARCHAR(255)` ASCII BIN | N | Google의 변경되지 않는 사용자 식별자 |
| `email` | `VARCHAR(320)` | N | 표시·연락용 프로필이며 로그인 동일성 판단에 사용하지 않음 |
| `display_name` | `VARCHAR(100)` | N | 화면 표시명 |
| `last_login_at` | `DATETIME(6)` | N | 마지막 인증 성공 시각 |
| `created_at` | `DATETIME(6)` | N | 생성 시각 |
| `updated_at` | `DATETIME(6)` | N | 수정 시각 |

- PK: `id`
- UNIQUE: `uk_members_provider_subject (oauth_provider, oauth_subject)`
- CHECK: `ck_members_oauth_provider (oauth_provider = 'GOOGLE')`
- 같은 Google 사용자의 동시 최초 로그인은 UNIQUE와 `INSERT ... ON DUPLICATE KEY UPDATE`로 한 회원에 수렴한다.
- 이메일은 바뀔 수 있으므로 고유 키로 사용하지 않는다.
- Google token, 자취선배 JWT, Refresh Token과 세션 데이터는 저장하지 않는다.
- 회원 탈퇴는 현재 범위 밖이므로 삭제 컬럼과 삭제 API가 없다.

## 매물

### `properties`

인증 회원이 등록한 후보 매물의 기본 정보, 발견 경로와 매물 단위 메모를 저장한다.

| 컬럼 | 타입 | NULL | 의미 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | N | 자동 증가 PK |
| `member_id` | `BIGINT` | N | 소유 회원 FK |
| `name` | `VARCHAR(50)` | N | 앞뒤 공백을 제거한 매물 이름 |
| `deposit_amount` | `BIGINT` | N | 원 단위 보증금 |
| `monthly_rent_amount` | `BIGINT` | N | 원 단위 월세 |
| `discovery_source_type` | `VARCHAR(20)` | N | `URL` 또는 `TEXT` |
| `discovery_source` | `VARCHAR(500)` | N | 앞뒤 공백을 제거한 발견 경로 원문 |
| `memo` | `VARCHAR(5000)` | N | 매물 단위 메모, 기본값 빈 문자열 |
| `memo_updated_at` | `DATETIME(6)` | Y | 최초 메모 저장 전에는 `NULL` |
| `last_activity_at` | `DATETIME(6)` | N | 목록 정렬용 최근 활동 시각 |
| `created_at` | `DATETIME(6)` | N | 생성 시각 |
| `updated_at` | `DATETIME(6)` | N | 기본 정보 또는 메모 수정 시각 |

- FK: `fk_properties_member (member_id) → members(id)`
- UNIQUE: `uk_properties_id_member (id, member_id)`. 후속 자식 테이블이 소유자까지 복합 FK로 참조할 수 있게 한다.
- CHECK: 발견 경로 유형은 `URL`, `TEXT`만 허용한다.
- CHECK: 보증금과 월세는 각각 0 이상 9,007,199,254,740,991 이하만 허용한다.
- INDEX: `idx_properties_member_activity (member_id, last_activity_at DESC, id DESC)`
- 회원당 매물 개수 고유 제약은 두지 않는다.
- 매물 조회·수정·메모·삭제 SQL은 `id`와 JWT에서 얻은 `member_id`를 함께 조건으로 사용한다.
- 매물은 모든 외부 사진 객체가 삭제되거나 이미 없음이 확인된 뒤 물리 삭제한다. `property_photos` 메타데이터와 `property_active_checklists` 연결은 FK cascade로 함께 삭제한다.
- 메모는 버전 없이 마지막으로 DB에 반영된 값이 남는다. `memo_version`과 `expectedVersion`은 사용하지 않는다.
- v1.1 하위 호환을 위해 `memo`, `memo_updated_at`을 삭제하지 않는다. API-106은 `additional_memo`·`saved_at`과 두 legacy 컬럼을 원자적으로 dual-write하고 API-103은 구조화 행이 없을 때 legacy 컬럼을 fallback으로 읽는다.

### `property_pre_visit_memos`

매물 방문 전 확인할 일곱 구조화 항목과 추가 메모를 매물마다 최대 한 행으로 저장한다. API-103·106의 메모 정본이며 cleanup 전까지 `properties.memo`, `properties.memo_updated_at`과 호환 운용한다.

| 컬럼 | 타입 | NULL | 의미 |
| --- | --- | --- | --- |
| `property_id` | `BIGINT` | N | 매물 식별자이자 PK |
| `member_id` | `BIGINT` | N | 매물과 같은 소유 회원 식별자 |
| `viewing_schedule` | `VARCHAR(200)` | N | 집 보기 일정 |
| `move_in_availability` | `VARCHAR(200)` | N | 입주 가능 시점 |
| `provisional_deposit` | `VARCHAR(200)` | N | 가계약금·예약금 확인 내용 |
| `room_options` | `VARCHAR(200)` | N | 옵션 확인 내용 |
| `maintenance_and_utilities` | `VARCHAR(200)` | N | 관리비·공과금 확인 내용 |
| `commute_time` | `VARCHAR(200)` | N | 통학·통근 시간 확인 내용 |
| `government_support` | `VARCHAR(200)` | N | 정부 지원·대출 확인 내용 |
| `additional_memo` | `VARCHAR(5000)` | N | 기존 자유 메모를 이어받는 추가 메모 |
| `saved_at` | `DATETIME(6)` | N | 마지막 명시적 저장 시각 |
| `created_at` | `DATETIME(6)` | N | 행 생성 시각 |
| `updated_at` | `DATETIME(6)` | N | 마지막 변경 시각 |

- PK: `property_id`로 매물마다 행을 최대 하나로 제한한다.
- FK: `fk_pre_visit_memos_property_owner (property_id, member_id) → properties(id, member_id) ON DELETE CASCADE`가 소유권과 매물 삭제 정리를 강제한다.
- 기존 매물마다 한 행을 만들고 `properties.memo`를 `additional_memo`로, `COALESCE(memo_updated_at, updated_at)`을 저장·수정 시각으로 backfill한다.
- 구조화 필드는 빈 문자열로 시작한다. 기존 `properties.memo`는 cleanup 전까지 보존한다.
- 쓰기는 매물 행을 `FOR UPDATE`로 잠근 뒤 구조화 행 전체 upsert와 legacy 컬럼 갱신을 한 트랜잭션에서 수행한다. 마지막으로 커밋된 요청이 최종값이며 메모 version은 사용하지 않는다.
- API-103은 구조화 행을 소유 회원 조건으로 `LEFT JOIN`한다. 행이 없으면 일곱 필드는 빈 문자열, `additional_memo`는 `properties.memo`, 저장 시각은 `COALESCE(properties.memo_updated_at, properties.updated_at)`으로 반환하고 DB를 변경하지 않는다.
- legacy `{content}` 쓰기는 기존 구조화 행을 먼저 읽어 일곱 필드를 보존하고 `additional_memo`만 바꾼다. 구조화 행이 없으면 빈 일곱 필드로 새 행을 만든다.
- `properties.memo`와 `properties.memo_updated_at` 제거는 v1.0 클라이언트 호환 관찰 뒤 별도 cleanup 이슈와 새 마이그레이션에서 수행한다.

## 매물 사진

### `property_photos`

비공개 객체 저장소의 사진과 소유 매물을 연결하는 메타데이터를 저장한다. 바이너리, 원본 파일명과 공개 URL은 저장하지 않는다.

| 컬럼 | 타입 | NULL | 의미 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | N | 자동 증가 PK |
| `property_id` | `BIGINT` | N | 소유 매물 식별자 |
| `member_id` | `BIGINT` | N | 매물과 같은 소유 회원 식별자 |
| `storage_key` | `VARCHAR(512)` ASCII BIN | N | 외부 저장소 내부 객체 키 |
| `content_type` | `VARCHAR(100)` | N | `image/jpeg`, `image/png`, `image/webp` 중 하나 |
| `size_bytes` | `BIGINT` | N | 1 이상 10,485,760 이하 바이트 크기 |
| `checksum_sha256` | `CHAR(64)` ASCII BIN | Y | 업로드 바이트의 SHA-256 |
| `created_at` | `DATETIME(6)` | N | 업로드 완료 시각과 정렬 기준 |

- FK: `fk_property_photos_property_owner (property_id, member_id) → properties(id, member_id) ON DELETE CASCADE`
- UNIQUE: `uk_property_photos_storage_key (storage_key)`
- CHECK: `content_type`은 JPEG·PNG·WebP MIME만 허용한다.
- CHECK: `size_bytes`는 1 이상 10 MiB 이하다.
- INDEX: `idx_property_photos_property_created (property_id, created_at, id)`. 매물별 업로드 순 목록과 미리보기에 사용한다.
- 저장 키는 `members/{memberId}/properties/{propertyId}/{uuid}` 형식이며 API에 노출하지 않는다.
- 매물당 30장 제한은 사진 메타데이터 트랜잭션이 `properties` 행을 `FOR UPDATE`로 잠근 뒤 개수를 세어 강제한다.
- 사진 업로드는 외부 객체 저장 뒤 짧은 DB 트랜잭션으로 메타데이터를 저장한다. DB 단계 실패 시 방금 저장한 객체를 동기 보상 삭제한다.
- 사진 삭제는 외부 `deleteIfExists` 성공 뒤 DB 메타데이터를 물리 삭제한다. 외부 객체가 이미 없는 경우는 성공으로 본다.
- 매물 삭제는 외부 객체 집합 삭제 뒤 매물 행을 잠그고 현재 키 집합을 다시 확인한 경우에만 수행한다.

## 제공 체크 항목과 프리셋

### `check_items`

서비스가 제공하는 단계별 기준 질문과 선택 안내를 저장한다. 사용자가 작성한 체크리스트와 분리해 제공 항목을 비활성화해도 기존 구성을 해석할 수 있게 한다.

| 컬럼 | 타입 | NULL | 의미 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | N | 안정적인 기준 항목 PK |
| `stage` | `VARCHAR(30)` | N | `ONLINE_PHONE`, `ON_SITE`, `PRE_CONTRACT` |
| `question` | `VARCHAR(500)` | N | 사용자에게 표시할 확인 질문 |
| `guide` | `VARCHAR(1000)` | Y | 선택 확인 안내 |
| `is_active` | `BOOLEAN` | N | 신규 검색·선택 가능 여부 |
| `created_at` | `DATETIME(6)` | N | 생성 시각 |
| `updated_at` | `DATETIME(6)` | N | 수정 시각 |

- UNIQUE: `uk_check_items_id_stage (id, stage)`는 단계 일치 복합 FK의 부모 키다.
- UNIQUE: `uk_check_items_stage_question (stage, question)`는 같은 단계의 질문 중복을 막는다.
- CHECK: `stage`는 세 단계만 허용한다.
- INDEX: `idx_check_items_stage_active (stage, is_active, id)`는 단계별 활성 항목의 안정적인 ID 순 검색에 사용한다.

### `checklist_presets`

주거 유형과 단계 조합별 읽기 전용 시작 템플릿의 루트를 저장한다.

| 컬럼 | 타입 | NULL | 의미 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | N | 자동 증가 PK |
| `preset_type` | `VARCHAR(20)` | N | `ONE_ROOM` 또는 `GOSHIWON` |
| `stage` | `VARCHAR(30)` | N | 체크 단계 |
| `name` | `VARCHAR(100)` | N | 내부 관리용 프리셋 이름 |
| `is_active` | `BOOLEAN` | N | 조회 가능 여부 |
| `created_at` | `DATETIME(6)` | N | 생성 시각 |
| `updated_at` | `DATETIME(6)` | N | 수정 시각 |

- UNIQUE: `uk_checklist_presets_type_stage (preset_type, stage)`는 유형·단계마다 프리셋을 하나로 제한한다.
- UNIQUE: `uk_checklist_presets_id_stage (id, stage)`는 단계 일치 복합 FK의 부모 키다.
- CHECK: `preset_type`과 `stage`는 정의된 값만 허용한다.
- v1.1은 `ONE_ROOM` 세 단계만 활성화한다. `GOSHIWON` 세 프리셋과 72개 매핑 행은 삭제하지 않고 비활성 상태로 보존한다.

### `checklist_preset_items`

프리셋의 기준 항목과 표시 순서를 저장한다.

| 컬럼 | 타입 | NULL | 의미 |
| --- | --- | --- | --- |
| `preset_id` | `BIGINT` | N | 프리셋 루트 FK |
| `check_item_id` | `BIGINT` | N | 제공 체크 항목 FK |
| `stage` | `VARCHAR(30)` | N | 두 부모와 같은 단계 |
| `item_order` | `SMALLINT UNSIGNED` | N | 1부터 시작하는 표시 순서 |

- PK: `(preset_id, check_item_id)`는 항목 중복을 막는다.
- UNIQUE: `uk_preset_items_order (preset_id, item_order)`는 순서 중복을 막는다.
- 복합 FK 두 개가 프리셋·기준 항목과 `stage` 일치를 강제한다.
- 프리셋 삭제는 구성 항목에 cascade하고 기준 항목 삭제는 restrict한다.

## 회원 체크리스트

### `checklists`

회원이 이름과 고정 단계를 붙여 만든 체크리스트 루트를 저장한다.

| 컬럼 | 타입 | NULL | 의미 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | N | 자동 증가 PK |
| `member_id` | `BIGINT` | N | 소유 회원 FK |
| `name` | `VARCHAR(50)` | N | 앞뒤 공백을 제거한 이름 |
| `stage` | `VARCHAR(30)` | N | 생성 후 바뀌지 않는 체크 단계 |
| `created_at` | `DATETIME(6)` | N | 생성 시각 |
| `updated_at` | `DATETIME(6)` | N | 이름·항목 전체 교체 시각 |

- FK: `fk_checklists_member (member_id) → members(id)`
- UNIQUE: `uk_checklists_id_member_stage (id, member_id, stage)`와 `uk_checklists_id_stage (id, stage)`는 후속 복합 FK의 부모 키다. 이름·회원·단계 조합에는 고유 제약을 두지 않는다.
- CHECK: trim한 이름 길이는 1~50이고 단계는 세 허용 값 중 하나다.
- INDEX: `idx_checklists_member_stage_updated (member_id, stage, updated_at DESC, id DESC)`는 회원·단계별 최근 수정 목록에 사용한다.
- 변경·삭제는 `id`와 JWT의 `member_id`로 루트를 `FOR UPDATE` 한 뒤 수행한다. 동시 전체 변경은 마지막으로 커밋된 요청이 남는다.

### `checklist_items`

회원 체크리스트가 선택한 제공 항목 또는 직접 작성한 질문과 사용자 지정 순서를 저장한다.

| 컬럼 | 타입 | NULL | 의미 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | N | 자동 증가 PK |
| `checklist_id` | `BIGINT` | N | 체크리스트 루트 FK |
| `origin` | `VARCHAR(20)` | N | `PROVIDED` 또는 `CUSTOM` |
| `check_item_id` | `BIGINT` | Y | 제공 항목이면 기준 체크 항목 FK |
| `custom_question` | `VARCHAR(200)` | Y | 사용자 작성 항목이면 질문 원문 |
| `stage` | `VARCHAR(30)` | N | 두 부모와 같은 단계 |
| `item_order` | `SMALLINT UNSIGNED` | N | 1부터 시작하는 사용자 지정 순서 |
| `created_at` | `DATETIME(6)` | N | 항목 생성 시각 |
| `updated_at` | `DATETIME(6)` | N | 항목 수정 시각 |

- PK: `id`는 제공 항목과 사용자 작성 항목을 같은 방식으로 방문 원본에 연결한다.
- UNIQUE: `uk_checklist_items_id_checklist (id, checklist_id)`는 항목과 부모 식별 조합을 제공한다.
- UNIQUE: `uk_checklist_items_provided (checklist_id, check_item_id)`는 같은 제공 항목 중복을 막고, `uk_checklist_items_order (checklist_id, item_order)`는 순서 중복을 막는다.
- 복합 FK 두 개가 체크리스트·제공 항목과 `stage` 일치를 강제한다.
- CHECK: `PROVIDED`이면 `check_item_id`만 있고, `CUSTOM`이면 trim한 1~200자 `custom_question`만 있도록 배타성을 강제한다.
- 체크리스트 삭제는 구성 항목에 cascade하고 제공 항목 삭제는 restrict한다.
- CUSTOM은 이 테이블의 로컬 행으로만 존재한다. `check_items`, `checklist_presets`, `checklist_preset_items`에는 삽입하지 않고 같은 CUSTOM 문구 중복에는 UNIQUE를 두지 않는다.
- 생성은 루트와 구성 항목을 같은 트랜잭션에서 저장한다. 전체 변경은 `checklists(id, member_id)` 루트를 `FOR UPDATE`로 잠그고 현재 항목도 잠근 뒤 검증·이름 변경·항목 diff를 한 트랜잭션으로 반영한다.
- diff는 최종 목록에서 빠진 행만 삭제하고 유지 행을 충돌하지 않는 임시 순서대로 옮긴 뒤 질문·최종 순서를 갱신한다. PROVIDED는 같은 `check_item_id`, CUSTOM은 요청한 소유 `id`를 유지하며 새 행에만 새 `id`를 발급한다.
- v1.0 `checkItemIds` 전체 변경은 기존 CUSTOM 행이 있으면 삭제 전에 409로 중단한다. 다른 체크리스트의 로컬 ID는 회원 정보나 질문을 노출하지 않고 찾을 수 없음으로 처리한다.
- 기존 v1.0 행은 자동 증가 `id`를 부여하고 모두 `PROVIDED`로 backfill한다.

## 매물 활성 체크리스트

### `property_active_checklists`

매물의 각 단계에서 다음 방문에 사용할 회원 체크리스트를 live 참조로 연결한다.

| 컬럼 | 타입 | NULL | 의미 |
| --- | --- | --- | --- |
| `property_id` | `BIGINT` | N | 소유 매물 식별자, PK 일부 |
| `member_id` | `BIGINT` | N | 매물과 체크리스트의 같은 소유 회원 |
| `stage` | `VARCHAR(30)` | N | 연결 단계, PK 일부 |
| `checklist_id` | `BIGINT` | N | 현재 선택한 회원 체크리스트 |
| `created_at` | `DATETIME(6)` | N | 최초 연결 시각 |
| `updated_at` | `DATETIME(6)` | N | 마지막 교체 시각 |

- PK: `(property_id, stage)`는 매물·단계마다 연결을 최대 하나로 제한한다. 체크리스트 ID에는 고유 제약을 두지 않아 여러 매물에서 재사용한다.
- FK: `fk_active_checklists_property_owner (property_id, member_id) → properties(id, member_id) ON DELETE CASCADE`가 매물 소유권과 매물 삭제 정리를 강제한다.
- FK: `fk_active_checklists_checklist_owner_stage (checklist_id, member_id, stage) → checklists(id, member_id, stage) ON DELETE CASCADE`가 체크리스트 소유권·단계를 강제하고 체크리스트 삭제 시 현재 연결만 제거한다.
- CHECK: `ck_active_checklists_stage`는 세 단계만 허용한다.
- INDEX: `idx_active_checklists_checklist (checklist_id)`는 API-303·305의 실제 `assignedPropertyCount` 집계와 체크리스트 삭제 FK 처리에 사용한다.
- 연결에는 이름과 항목을 복제하지 않는다. API-103은 현재 `checklists`, `checklist_items`를 조인해 최신 이름과 항목 수를 반환한다.
- 설정·교체·해제는 먼저 본인 `properties` 행을 `FOR UPDATE`로 잠근다. 설정·교체는 이어 본인 `checklists` 루트를 잠그고 URL 단계 일치를 검증한 뒤 소유권 조건이 포함된 upsert를 수행한다.
- 같은 매물·단계의 동시 설정과 해제는 매물 잠금 순서로 직렬화된다. 체크리스트 수정·삭제와 경합하면 체크리스트 루트 잠금 및 두 복합 FK가 부분 연결과 고아 연결을 막는다.
- 연결 변경 트랜잭션이 MySQL deadlock·lock timeout으로 실패하면 애플리케이션 서비스 경계에서 20ms 단위의 짧은 백오프로 최대 3회 시도한다. 업무 오류와 다른 DB 오류는 재시도하지 않는다.

## 방문과 체크 결과

### `visits`

한 매물을 확인한 한 차례 방문의 상태와 시각을 저장한다.

| 컬럼 | 타입 | NULL | 의미 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | N | 자동 증가 PK |
| `property_id` | `BIGINT` | N | 방문 매물 |
| `member_id` | `BIGINT` | N | 매물과 같은 소유 회원 |
| `status` | `VARCHAR(20)` | N | `IN_PROGRESS`, `COMPLETED` |
| `started_at` | `DATETIME(6)` | N | 방문 시작 시각 |
| `completed_at` | `DATETIME(6)` | Y | 최초 완료 시각 |
| `updated_at` | `DATETIME(6)` | N | 상태 또는 항목 마지막 변경 시각 |

- FK: `fk_visits_property_owner (property_id, member_id) → properties(id, member_id) ON DELETE CASCADE`가 소유권과 매물 삭제 정리를 강제한다.
- CHECK: `ck_visits_status`, `ck_visits_completion`이 상태 허용값과 상태·완료 시각 조합을 강제한다.
- INDEX: `idx_visits_property_started (property_id, started_at DESC, id DESC)`는 API-501과 최근 방문 조회에 사용하고 `idx_visits_member_started`는 회원별 완료 방문 관찰에 사용한다.
- 같은 매물에 여러 방문을 허용하므로 매물 고유 제약을 두지 않는다. 최근 방문은 `started_at DESC, id DESC` 첫 행이다.
- 완료는 항목 수정 잠금이 아니다. 완료 후 항목을 바꿔도 상태와 최초 `completed_at`은 유지한다.

### `visit_stage_snapshots`

방문 시작 당시 활성 체크리스트의 단계와 이름을 독립 스냅샷으로 저장한다.

| 컬럼 | 타입 | NULL | 의미 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | N | 자동 증가 PK |
| `visit_id` | `BIGINT` | N | 방문 루트 FK |
| `stage` | `VARCHAR(30)` | N | 방문 당시 단계 |
| `source_checklist_id` | `BIGINT` | Y | 원본 추적 ID, 삭제 시 NULL |
| `checklist_name` | `VARCHAR(50)` | N | 방문 당시 이름 |
| `created_at` | `DATETIME(6)` | N | 복제 시각 |

- UNIQUE: `uk_visit_snapshots_visit_stage (visit_id, stage)`는 한 방문의 단계 중복을 막고 `uk_visit_snapshots_id_stage`는 자식 단계 복합 FK의 부모 키다.
- FK: 방문 삭제는 cascade하고 원본 체크리스트 삭제는 `source_checklist_id`만 `SET NULL`한다.
- 활성 단계가 하나도 없거나 활성 체크리스트 구성이 비어 있으면 방문을 만들지 않는다. 생성 후 구조 변경 API는 없다.

### `visit_check_items`

방문 당시 질문·안내·순서와 이후 서로 독립적으로 자동 저장되는 확인 상태·인라인 메모를 저장한다.

| 컬럼 | 타입 | NULL | 의미 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | N | 자동 증가 PK |
| `visit_stage_snapshot_id` | `BIGINT` | N | 단계 스냅샷 FK |
| `stage` | `VARCHAR(30)` | N | 부모·원본과 같은 단계 |
| `origin` | `VARCHAR(20)` | N | `PROVIDED` 또는 `CUSTOM` |
| `source_checklist_item_id` | `BIGINT` | Y | 방문을 만든 체크리스트 항목 추적 ID, 원본 삭제 시 NULL |
| `source_check_item_id` | `BIGINT` | Y | 제공 항목이면 기준 체크 항목 추적 ID |
| `question_snapshot` | `VARCHAR(500)` | N | 방문 당시 질문 |
| `guide_snapshot` | `VARCHAR(1000)` | Y | 방문 당시 확인 안내 |
| `item_order` | `SMALLINT UNSIGNED` | N | 방문 당시 순서 |
| `status` | `VARCHAR(20)` | N | `GOOD`, `CAUTION`, `UNCONFIRMED` |
| `version` | `BIGINT` | N | 논리 `statusVersion`인 상태 자동 저장 낙관적 잠금 버전, 기본 0 |
| `status_saved_at` | `DATETIME(6)` | Y | 논리 `statusSavedAt`, 상태가 마지막으로 저장된 시각 |
| `inline_memo` | `VARCHAR(200)` | N | 줄바꿈 없는 인라인 메모, 기본 빈 문자열 |
| `memo_version` | `BIGINT` | N | 메모 자동 저장의 독립 낙관적 잠금 버전, 기본 0 |
| `memo_updated_at` | `DATETIME(6)` | Y | 논리 `memoSavedAt`, 메모가 마지막으로 저장된 시각 |
| `created_at` | `DATETIME(6)` | N | 생성 시각 |
| `updated_at` | `DATETIME(6)` | N | 마지막 항목 변경 시각 |

- UNIQUE: `uk_visit_items_source`, `uk_visit_items_source_checklist_item`, `uk_visit_items_order`가 같은 단계 스냅샷의 제공 항목·원본 체크리스트 항목·순서 중복을 각각 막는다.
- FK: 원본 체크리스트 항목 삭제는 `source_checklist_item_id`만 `SET NULL`하고 origin·질문·안내·순서·상태를 보존한다. 복합 FK가 단계 스냅샷과 제공 기준 항목의 단계 일치를 강제한다. 단계 스냅샷 삭제는 cascade한다.
- CHECK: 단계·상태·origin 허용값, 제공·사용자 원본 배타성, 1 이상 순서, 0 이상 상태·메모 version, 200자 이하와 CR·LF가 없는 인라인 메모를 강제한다.
- 상태 저장은 매물 → 방문 순으로 루트를 잠근 뒤 `member_id + visit_id + item_id + expectedStatusVersion` 조건부 단일 행 UPDATE를 수행한다. `status`, `version`, `status_saved_at`, 공통 `updated_at`만 변경하고 메모 세 필드는 SQL에 포함하지 않는다. 성공하면 상태 version을 1 증가시키고 방문·매물 활동 시각과 상태 집계를 같은 트랜잭션에서 반영한다.
- 메모 저장도 같은 잠금·소유권 순서를 따르되 `memo_version=expectedMemoVersion`만 비교한다. `inline_memo`, `memo_version`, `memo_updated_at`, 공통 `updated_at`만 변경하고 상태 세 필드는 SQL에 포함하지 않는다. 성공하면 메모 version을 1 증가시키고 방문·매물 활동 시각만 갱신하며 상태 집계는 다시 계산하지 않는다.
- 공통 `updated_at`은 항목 활동 시각일 뿐 API의 상태·메모 저장 시각 정본이 아니다. API-503은 `status_saved_at`을 읽되 부분 배포 행의 `NULL`만 `updated_at`으로 fallback하고, `memo_updated_at`은 최초 메모 저장 전 `NULL`로 그대로 반환한다.
- 같은 채널의 같은 expected version 동시 요청은 정확히 하나만 성공한다. 상태와 메모가 각각 현재 version으로 경합하면 매물·방문 잠금 때문에 직렬화되더라도 서로 다른 version과 UPDATE SET을 사용하므로 둘 다 성공하고 두 값을 보존한다. 조건부 UPDATE 0행 뒤의 항목 조회도 같은 회원·방문 소유권 조건으로 Not Found와 채널별 충돌을 구분한다.
- 방문 생성은 매물과 체크리스트 루트를 순서대로 잠그고 방문 루트·1~3개 단계·모든 PROVIDED·CUSTOM 항목을 한 트랜잭션에서 삽입한다. PROVIDED는 로컬·전역 출처와 현재 질문·안내를, CUSTOM은 로컬 출처·질문과 `NULL` 전역 출처·안내를 복사한다. 모든 항목은 `UNCONFIRMED`, 상태 version 0, 빈 인라인 메모, 메모 version 0으로 시작한다. deadlock·lock timeout만 짧은 백오프로 최대 3회 새 트랜잭션에서 시도한다.
- v1.0 행은 모두 `PROVIDED`로 표시하고 원본 체크리스트 항목 ID를 연결한다. 기존 `version`과 질문·안내 스냅샷을 보존하며 `status_saved_at=updated_at`, `inline_memo=''`, `memo_version=0`, `memo_updated_at=NULL`로 backfill한다.

## 스키마 마이그레이션

- Flyway가 `src/main/resources/db/migration`의 버전 SQL을 애플리케이션 시작 전에 적용하고 `flyway_schema_history`에 버전·checksum·성공 여부를 기록한다.
- V1은 v1.0의 12개 테이블과 제공 체크 항목 72개, `ONE_ROOM`·`GOSHIWON` × 세 단계 프리셋 6개를 보존한다.
- V2~V4는 v1.1 구조 확장, 기존 데이터 backfill, 제약 확정을 수행해 제품 테이블을 13개로 만든다.
- BE-v1.1-4는 V2~V4의 `version`, `status_saved_at`, `inline_memo`, `memo_version`, `memo_updated_at`과 CHECK를 그대로 사용하므로 신규 migration을 추가하지 않는다. 적용된 V1~V4 파일과 checksum을 변경하지 않는다.
- 제공 항목의 안정적인 ID는 유지하고 현재 질문만 사용자 판단형으로 갱신한다. 이미 생성된 방문의 질문·안내 스냅샷은 바꾸지 않는다.
- 빈 DB는 V1부터 적용하고 pre-Flyway v1.0 DB는 검증·백업·복구 리허설 뒤 버전 1을 명시적으로 baseline한다.
- 적용된 파일은 수정하지 않고 다음 버전의 순방향 마이그레이션을 추가한다. 상세 절차는 [데이터베이스 마이그레이션 가이드](../guides/database-migrations.md)를 따른다.
- `SPRING_SESSION`, `SPRING_SESSION_ATTRIBUTES`, Refresh Token, 파일 삭제 작업 큐와 후속 인프라 테이블은 만들지 않는다.

## 모델 기록 방법

테이블이 추가되면 다음 내용을 기록한다.

| 항목 | 내용 |
| --- | --- |
| 테이블 | 이름과 책임 |
| 주요 컬럼 | 타입, null 허용 여부와 의미 |
| 관계 | 참조 대상과 카디널리티 |
| 제약조건 | PK, FK, UNIQUE와 CHECK |
| 인덱스 | 대상 쿼리와 선택 근거 |
| 삭제 정책 | 물리 삭제 또는 soft delete 이유 |

후속 테이블도 같은 양식으로 기록한다. 운영 데이터가 존재한 뒤의 변경 절차와 마이그레이션 도구는 도입 전에 별도로 결정한다.
