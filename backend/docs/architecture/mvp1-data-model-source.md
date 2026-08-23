# MVP1 전달 데이터 모델 원문

- 문서 성격: 시점 고정
- 갱신 정책: 2026년 8월 24일 사용자가 전달한 MVP1 ERD 표를 보존하므로 수정하지 않는다

## 문서의 위치

이 문서는 MVP2 설계의 입력 자료다. 실제 MVP1 스키마는 `mvp1-baseline` 태그의 마이그레이션, 현재 통합 스키마는 [`001-schema.sql`](../../src/main/resources/db/init/001-schema.sql)을 따른다. 전달 표의 오타와 제약 누락은 원문의 일부로 보존하고, MVP2 채택 모델은 [`mvp2-data-model.md`](mvp2-data-model.md)에서 정한다.

## `members` — 회원

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 회원 식별자 |
| `email` | VARCHAR(320) | NOT NULL | 검증한 이메일 |
| `name` | VARCHAR(100) | NOT NULL | 표시 이름 |
| `last_login_at` | DATETIME(6) | NOT NULL | 마지막 로그인 시각 |
| `created_at` | DATETIME(6) | NOT NULL | 생성 시각 |
| `updated_at` | DATETIME(6) | NOT NULL | 수정 시각 |

## `properties` — 후보 매물

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 매물 식별자 |
| `member_id` | BIGINT UNSIGNED | FK, NOT NULL | 매물 소유 회원 |
| `name` | VARCHAR(50) | NOT NULL | 매물 이름 |
| `deposit_amount` | BIGINT UNSIGNED | NULL | 보증금, default 0 |
| `monthly_rent_amount` | BIGINT UNSIGNED | NULL | 월세, default 0 |
| `discovery_source` | VARCHAR(500) | NULL | URL 또는 일반 텍스트 발견 경로 |

- `member_id`는 `members.id`를 참조한다.
- 한 회원은 활성 매물을 최대 30개까지 등록한다.
- 매물을 삭제하면 사진 메타데이터, 메모와 매물 체크 기록을 함께 삭제한다.

## `property_photos` — 매물 사진 메타데이터

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 사진 식별자 |
| `property_id` | BIGINT UNSIGNED | FK, NOT NULL | 대상 매물 |
| `storage_key` | VARCHAR(500) | NOT NULL, UNIQUE | 비공개 객체 저장 키 |
| `content_type` | VARCHAR(50) | NOT NULL | JPG, JPEG, PNG, WebP, HEIC 타입 |
| `size_bytes` | BIGINT UNSIGNED | NOT NULL | 파일 크기 |
| `created_at` | DATETIME(6) | NOT NULL | 업로드 완료 시각 |

사진 원본은 외부 비공개 객체 저장소에 두고 DB에는 메타데이터만 저장한다.

## `main_property_photos` — 대표 사진

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 대표 사진 관계 식별자 |
| `property_id` | BIGINT UNSIGNED | FK, NOT NULL | 대상 매물 |
| `property_photos_id` | BIGINT UNSIGNED | FK, NOT NULL | 대상 사진 |

전달 제약은 `UNIQUE(property_id, property_photos_id)`다.

## `property_memos` — 매물 메모 루트

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 메모 식별자 |
| `property_id` | BIGINT UNSIGNED | FK, NOT NULL, UNIQUE | 메모 대상 매물 |
| `free_memo` | VARCHAR(2000) | NOT NULL, DEFAULT '' | 자유 메모 |

## `property_memo_items` — 구조화 메모 항목

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 항목 식별자 |
| `property_memo_id` | BIGINT UNSIGNED | FK, NOT NULL | 소속 메모 |
| `system_meno_id` | BIGINT UNSIGNED | NOT NULL | 시스템 항목 식별자 |
| `label` | VARCHAR(30) | NOT NULL | 항목명 |
| `content` | VARCHAR(200) | NOT NULL, DEFAULT '' | 항목 내용 |

- 한 메모에는 최대 20개 항목을 저장한다.
- 전체 저장 요청마다 항목 배열을 요청 상태로 교체한다.

## `system_memo_items` — 시스템 메모 항목

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 항목 식별자 |
| `label` | VARCHAR(30) | NOT NULL | 항목명 |
| `display_order` | SMALLINT UNSIGNED | NOT NULL | 표시 순서 |
| `deleted_at` | DATETIME(6) | NULL | 삭제 시간 |

## `system_check_items` — 시스템 체크 항목

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 시스템 항목 식별자 |
| `stage` | VARCHAR(30) | NOT NULL | `ONLINE_PHONE`, `ON_SITE`, `PRE_CONTRACT` |
| `item_type` | VARCHAR(20) | NOT NULL | `CORE`, `OPTIONAL` |
| `question` | VARCHAR(200) | NOT NULL | 확인 질문 |
| `deleted_at` | DATETIME(6) | NULL | 삭제 시간 |

비활성 항목은 신규 선택에서 제외하지만 기존 사용자 체크리스트와 매물 기록에서는 유지한다.

## `user_checklists` — 사용자 체크리스트

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 체크리스트 식별자 |
| `member_id` | BIGINT UNSIGNED | FK, NOT NULL | 소유 회원 |
| `name` | VARCHAR(50) | NOT NULL | 이름 |
| `stage` | VARCHAR(30) | NOT NULL | 생성 후 변경할 수 없는 단계 |

## `user_checklist_items` — 사용자 체크리스트 구성 항목

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 구성 항목 식별자 |
| `user_checklist_id` | BIGINT UNSIGNED | FK, NOT NULL | 소속 체크리스트 |
| `system_check_item_id` | BIGINT UNSIGNED | FK, NOT NULL | 시스템 체크 항목 |
| `display_order` | SMALLINT UNSIGNED | NOT NULL | 사용자 지정 표시 순서 |

- `(user_checklist_id, system_check_item_id)`와 `(user_checklist_id, display_order)`는 각각 UNIQUE다.

## `property_checklists` — 매물에 적용된 단계별 체크리스트

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 매물 체크리스트 식별자 |
| `property_id` | BIGINT UNSIGNED | FK, NOT NULL | 적용 대상 매물 |
| `user_checklist_id` | BIGINT UNSIGNED | FK, NULL | 원본 체크리스트 |
| `checklist_name` | VARCHAR(50) | NOT NULL | 스냅샷 이름 |
| `stage` | VARCHAR(30) | NOT NULL | 적용 단계 |

- 원본이 없어져도 스냅샷을 유지하도록 `user_checklist_id`는 NULL을 허용한다.
- `(property_id, stage)`는 UNIQUE다.

## `property_checklist_items` — 매물 체크 항목

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 매물 체크 항목 식별자 |
| `property_checklist_id` | BIGINT UNSIGNED | FK, NOT NULL | 소속 매물 체크리스트 |
| `system_check_item_id` | BIGINT UNSIGNED | FK, NOT NULL | 시스템 항목 ID |
| `display_order` | SMALLINT UNSIGNED | NOT NULL | 적용 당시 표시 순서 |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'UNCONFIRMED' | 상태 |
| `memo` | VARCHAR(500) | NOT NULL, DEFAULT '' | 항목별 메모 |
| `question` | VARCHAR(200) | NOT NULL | 적용 당시 질문 |

- `(property_checklist_id, system_check_item_id)`와 `(property_checklist_id, display_order)`는 각각 UNIQUE다.
- 원본 질문 변경과 비활성화는 스냅샷에 영향을 주지 않는다.
- 교체 시 같은 `system_check_item_id`의 상태와 메모만 승계하고 모든 항목에 새 ID를 발급한다.

## 전달본과 실제 구현의 차이

- 실제 `members`에는 `last_login_at`이 없다.
- 실제 `property_photos`에는 소유자 검증용 `member_id`와 체크섬 컬럼이 있다.
- 실제 `user_checklist_items`는 단계·유형·질문도 스냅샷으로 저장한다.
- 실제 `property_memo_items` 컬럼명은 `system_memo_item_id`이고 표시 순서가 있다.
- 대표 사진 전달 제약은 매물당 한 장을 보장하지 못한다.
