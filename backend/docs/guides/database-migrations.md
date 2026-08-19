# 데이터베이스 마이그레이션

- 문서 성격: 파생
- 대조 대상: `build.gradle`, `src/main/resources/application.yml`, `src/main/resources/db/migration`

이 문서는 Flyway 마이그레이션의 작성·검증·배포·복구 절차를 관리하는 정본이다. 로컬 실행은 [로컬 개발](local-development.md), 배포 순서는 [배포](../operations/deployment.md), 장애 뒤 선택은 [롤백](../operations/rollback.md)을 함께 참고한다.

## 1. 현재 마이그레이션

| 버전 | 책임 |
| --- | --- |
| `V1__create_v1_0_schema.sql` | v1.0의 12개 제품 테이블, 제공 항목 72개와 프리셋 6개를 만든다 |
| `V2__expand_v1_1_schema.sql` | 사전 메모 테이블과 사용자 항목·방문 메모용 컬럼·키를 추가한다 |
| `V3__backfill_v1_1_data.sql` | 기존 메모·항목·방문 데이터를 보정하고 GOSHIWON 프리셋을 비활성화하며 제공 질문을 갱신한다 |
| `V4__enforce_v1_1_constraints.sql` | 제공·사용자 항목 배타성, 메모 길이·개행·버전 제약을 확정한다 |
| `V5__extend_schema_for_erd.sql` | ERD 기준 회원·매물·메모·시스템 항목·사용자 체크리스트·매물 스냅샷을 추가하고 Refresh Token·마지막 로그인 시각을 제거한다 |

`V1~V4`는 레거시 스키마다. 새 개발 DB에는 V1~V5가 순서대로 실행되며, V5가 현재 ERD 기준의 최종 전환 단계다. V5·V6·V7을 별도 파일로 나누지 않고 하나의 마이그레이션으로 관리한다.

## 2. 작성 규칙

- 적용됐거나 공유된 마이그레이션은 수정·삭제·이름 변경하지 않는다. 정정은 더 높은 버전의 새 파일로 작성한다.
- 스키마 확장, 데이터 backfill, 파괴적 정리는 가능한 한 다른 버전으로 나눈다.
- 여러 배포 버전이 함께 동작해야 하면 먼저 nullable 컬럼·새 테이블을 추가하고 애플리케이션 전환 뒤 별도 작업에서 정리한다.
- `baseline-on-migrate=false`, `clean-disabled=true`, `out-of-order=false`, `validate-on-migrate=true`를 기본으로 유지한다.
- checksum 불일치나 실패 이력이 생기면 원인을 확인하기 전에 `repair`하지 않는다. 파일을 과거 checksum에 맞춰 되돌리거나 승인된 후속 마이그레이션과 장애 기록으로 해결한다.
- MySQL DDL은 암시적 커밋될 수 있으므로 한 파일이 전부 원자적으로 취소된다고 가정하지 않는다.
- 운영 DB에서 직접 파일을 골라 실행하거나 `flyway_schema_history`를 수동 수정하지 않는다.

## 3. 빈 DB 검증

애플리케이션을 시작하면 V1부터 최신 버전까지 자동 적용한다.

```bash
./gradlew bootRun
```

적용 뒤 이력을 확인한다.

```sql
SELECT installed_rank, version, description, type, success, installed_on
FROM flyway_schema_history
ORDER BY installed_rank;

SELECT COUNT(*) AS product_table_count
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_type = 'BASE TABLE'
  AND table_name <> 'flyway_schema_history';
```

현재 정상 결과는 성공한 V1~V5 다섯 행이다. 애플리케이션을 재시작해도 `installed_rank`가 늘지 않아야 한다.

## 4. 기존 pre-Flyway v1.0 DB 전환

### 4.1 쓰기 중단과 사전 확인

애플리케이션 쓰기를 중단하고 다음 조건을 모두 확인한다.

- 대상 DB와 환경을 두 명이 교차 확인한다.
- MySQL 버전, 문자셋과 collation이 현재 운영 기준과 호환된다.
- `flyway_schema_history`가 없고 제품 테이블이 정확히 12개다.
- 테이블 이름이 V1의 `members`, `properties`, `property_photos`, `check_items`, `checklist_presets`, `checklist_preset_items`, `checklists`, `checklist_items`, `property_active_checklists`, `visits`, `visit_stage_snapshots`, `visit_check_items`와 일치한다.
- 회원·매물·사진·체크리스트·방문·스냅샷·항목의 행 수와 주요 ID 범위를 기록한다.
- V1과 다른 수동 스키마 변경이나 고아 FK 데이터가 없음을 확인한다.

```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_type = 'BASE TABLE'
ORDER BY table_name;

SELECT 'members' AS table_name, COUNT(*) AS row_count FROM members
UNION ALL SELECT 'properties', COUNT(*) FROM properties
UNION ALL SELECT 'property_photos', COUNT(*) FROM property_photos
UNION ALL SELECT 'checklists', COUNT(*) FROM checklists
UNION ALL SELECT 'checklist_items', COUNT(*) FROM checklist_items
UNION ALL SELECT 'visits', COUNT(*) FROM visits
UNION ALL SELECT 'visit_stage_snapshots', COUNT(*) FROM visit_stage_snapshots
UNION ALL SELECT 'visit_check_items', COUNT(*) FROM visit_check_items;
```

하나라도 다르면 baseline하지 않고 차이를 조사한다.

### 4.2 백업과 복구 리허설

일관된 논리 백업을 만들고 완료 코드와 파일 크기·해시를 기록한다. 다음 예시는 `backend/.env`를 로드한 로컬 Compose 절차다.

```bash
set -a
source .env
set +a
docker compose exec -T mysql \
  mysqldump --single-transaction --routines --triggers \
  -u"$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" > moca-v1-before-flyway.sql
shasum -a 256 moca-v1-before-flyway.sql
```

백업 파일이 있다는 사실만으로 진행하지 않는다. 운영 DB와 분리된 전용 DB에 복구하고, 앞에서 기록한 행 수·주요 ID·관계와 애플리케이션 v1.0 health를 확인한다. 복구 리허설의 소요 시간과 담당자도 기록한다.

```bash
docker compose exec -T mysql \
  mysql -u"$DB_USERNAME" -p"$DB_PASSWORD" \
  -e "CREATE DATABASE moca_restore_rehearsal CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci"
docker compose exec -T mysql \
  mysql -u"$DB_USERNAME" -p"$DB_PASSWORD" moca_restore_rehearsal \
  < moca-v1-before-flyway.sql
```

기존 이름의 리허설 DB가 있으면 자동으로 삭제하지 말고 별도 이름을 사용한다. 운영 환경에서는 플랫폼의 스냅샷·시점 복구 정책도 함께 확인한다.

### 4.3 버전 1 baseline

백업 복구가 검증된 정확한 v1.0 DB에서만 다음 한 번의 실행으로 버전 1을 baseline한다. `target=1`은 같은 실행에서 V2 이후가 적용되지 않도록 경계를 고정한다.

```bash
./gradlew bootRun --args='--spring.flyway.baseline-on-migrate=true --spring.flyway.baseline-version=1 --spring.flyway.baseline-description=v1.0-pre-Flyway-schema --spring.flyway.target=1'
```

기동 후 다음 쿼리에서 버전 1의 `BASELINE` 성공 행 하나만 생겼는지 확인하고 애플리케이션을 종료한다. 이 옵션을 환경변수나 공용 설정에 남기지 않는다.

```sql
SELECT installed_rank, version, description, type, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

### 4.4 v1.1 적용과 검증

기본 설정으로 애플리케이션을 다시 시작해 V2~V5를 적용한다.

```bash
./gradlew bootRun
curl --fail http://localhost:8080/actuator/health
```

배포를 계속하기 전에 다음을 확인한다.

- history에 `BASELINE`, V2, V3, V4, V5가 모두 `success=1`로 기록된다.
- 사전 기록한 기존 테이블 행 수, ID와 부모·자식 관계가 유지된다.
- `property_pre_visit_memos` 행 수가 `properties`와 같고 기존 `properties.memo`가 `additional_memo`로 복사된다.
- 기존 `checklist_items`와 `visit_check_items`는 `origin='PROVIDED'`이며 방문 원본 항목 연결이 채워진다.
- 방문 질문·안내 스냅샷과 상태 `version`은 바뀌지 않고 `status_saved_at=updated_at`, `inline_memo=''`, `memo_version=0`이다.
- GOSHIWON 프리셋과 매핑 행은 남아 있고 프리셋만 비활성 상태다.
- 제공 질문은 안정적인 ID를 유지한 채 갱신되고 기존 방문 질문 스냅샷은 유지된다.
- ERD 기준 신규 테이블이 존재하고 Refresh Token 테이블과 회원 `last_login_at` 컬럼이 없으며 health가 `UP`이다.

## 5. 실패 대응

- V1 baseline 전 실패는 DB를 변경하지 않은 상태인지 확인하고 원인을 해결한다.
- V2~V5 도중 실패하면 애플리케이션을 중단하고 쓰기를 계속 차단한다. MySQL DDL 부분 적용 여부와 history를 먼저 보존한다.
- 새 쓰기가 없고 복구 리허설이 끝난 백업이 있으면 대상 DB를 격리한 뒤 승인된 복구 절차로 전환 전 시점에 복구한다.
- 새 쓰기가 들어왔다면 백업 복구는 그 쓰기를 잃을 수 있다. 영향 데이터를 보존하고 후속 순방향 마이그레이션으로 고칠지 결정한다.
- 실패 이력을 숨기기 위해 `repair`, history 수정, 적용 파일 편집을 먼저 수행하지 않는다.
- 상세 선택 기준은 [롤백](../operations/rollback.md)을 따른다.

## 6. v1.1 이후 정리 경계

V1~V4는 레거시 이력으로 보존하고 수정하지 않는다. V5는 새 개발 DB 기준으로만 사용한다. 이미 V5·V6·V7을 적용한 DB에서 파일을 합치면 Flyway checksum 오류가 발생하므로, 해당 환경은 기존 파일을 유지하거나 별도 후속 마이그레이션으로 전환한다.
