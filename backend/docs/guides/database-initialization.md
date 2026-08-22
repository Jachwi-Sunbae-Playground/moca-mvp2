# 데이터베이스 초기화

- 문서 성격: 파생
- 대조 대상: `backend/src/main/resources/db/init/`, `backend/compose.yaml`, 테스트 설정

MVP2 DB는 기존 DB를 변경하지 않고 빈 MySQL에 현재 스키마를 한 번에 만든다. 결정 배경과 재검토 조건은 [ADR-0009](../adr/0009-use-disposable-database-schema.md)를 따른다.

## 정본

| 파일 | 책임 |
| --- | --- |
| `db/init/001-schema.sql` | 현재 애플리케이션이 사용하는 테이블·인덱스·제약 |
| `db/init/002-seed.sql` | 로컬과 테스트에서 사용하는 시스템 기본 항목 |

과거 Flyway SQL은 새 레포에서 관리하지 않는다. MVP1 당시의 변경 과정은 `mvp1-baseline` 태그에서 확인한다.

## 로컬 초기화

`docker compose up -d`는 MySQL 데이터 볼륨이 비어 있을 때만 `/docker-entrypoint-initdb.d`의 두 파일을 실행한다. 이미 만들어진 볼륨에서는 SQL 파일을 바꿔도 자동 반영하지 않는다.

현재 스키마로 다시 만들 필요가 있을 때만 다음 순서로 진행한다.

```bash
docker compose down
docker compose down -v
docker compose up -d
```

`docker compose down -v`는 로컬 MySQL과 MinIO 볼륨의 데이터를 모두 삭제한다. 보존할 데이터가 없는지 확인한 뒤 명시적으로 실행한다. 애플리케이션 시작이나 일반 개발 명령에서 자동 초기화하지 않는다.

## 스키마 변경

1. `001-schema.sql`을 현재 코드가 기대하는 최종 상태로 수정한다.
2. 시스템 기본 항목이 바뀌면 `002-seed.sql`을 수정한다.
3. 기존 로컬 데이터를 버려도 되는지 확인하고 새 볼륨으로 초기화한다.
4. 백엔드 전체 테스트를 실행한다.
5. 코드 변경과 스키마·관련 문서를 같은 PR에 포함한다.

테스트 프로필은 두 파일을 빈 Testcontainers MySQL에 적용한다. 운영 프로필과 일반 애플리케이션 기동에서는 Spring SQL 초기화를 실행하지 않는다.

## 운영 전환

첫 운영 DB는 서비스 시작 전에 같은 스키마와 기본 데이터를 한 번 적용한다. 이후 실제 사용자 데이터가 생기면 단일 스키마 파일을 직접 다시 실행하거나 DB를 초기화하지 않는다. 그 시점에는 [ADR-0009의 재검토 조건](../adr/0009-use-disposable-database-schema.md#재검토-조건)에 따라 Flyway 같은 순방향 마이그레이션 방식을 다시 선택한다.
