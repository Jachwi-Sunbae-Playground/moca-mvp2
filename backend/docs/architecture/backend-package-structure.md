# 백엔드 패키지 구조

## 기본 구조

기술 레이어보다 업무 도메인을 먼저 나누고 각 도메인 안에서 레이어를 구분한다.

```text
com.jachwisunbae
├── member
│   ├── controller
│   │   └── dto
│   │       ├── request
│   │       └── response
│   ├── service
│   │   └── dto
│   │       ├── command
│   │       └── result
│   ├── repository
│   ├── domain
│   └── client
├── property
│   ├── controller
│   │   └── dto
│   │       ├── request
│   │       └── response
│   ├── service
│   │   └── dto
│   │       ├── command
│   │       └── result
│   ├── repository
│   ├── domain
│   └── storage
├── checklist
│   ├── controller
│   │   └── dto
│   │       ├── request
│   │       └── response
│   ├── service
│   │   └── dto
│   │       ├── command
│   │       └── result
│   ├── repository
│   └── domain
├── visit
│   ├── controller
│   │   └── dto
│   │       ├── request
│   │       └── response
│   ├── service
│   │   └── dto
│   │       ├── command
│   │       └── result
│   ├── repository
│   └── domain
└── common
    ├── config
    ├── exception/{client,server,errorcode}
    ├── page
    ├── response
    ├── security
    ├── resolver
    └── observability
```

현재 구현된 업무 패키지는 Google 인증과 회원 조회를 담당하는 `member`, 매물 목록·등록·상세·수정·삭제·메모·사진을 담당하는 `property`, 제공 항목·프리셋·회원 체크리스트 관리를 담당하는 `checklist`, 방문 스냅샷·상태와 인라인 메모 독립 자동 저장·완료를 담당하는 `visit`다.

## 의존 방향

```text
controller → service → repository
                 ↓
               domain
```

- Controller는 Repository를 직접 호출하지 않는다.
- Repository는 Service와 Controller를 참조하지 않는다.
- Domain 객체는 Controller DTO를 참조하지 않는다.
- Service는 Controller Request·Response DTO를 참조하지 않는다.
- 도메인 간 협력이 필요하면 해당 도메인의 Service를 통해 수행한다.
- 도메인 간 순환 의존이 발생하면 책임과 경계를 재검토한다.
- `property`는 활성 연결 때 `ChecklistReferenceQueryService`만 호출하고 `visit`는 `PropertyAccessService`와 `ChecklistSnapshotSourceService`만 호출한다. 반대 방향 호출은 만들지 않는다.

## 패키지별 책임

### Controller

- HTTP 요청 역직렬화
- Bean Validation
- 인증된 사용자 정보 추출
- Request를 Command로 변환
- Service 호출
- Result를 Response로 변환
- HTTP 상태 코드와 Header 결정

Controller에는 비즈니스 판단을 작성하지 않는다.

### Service

- 사용자 Use Case 실행
- 트랜잭션 관리
- Domain 객체와 Repository의 실행 순서 조율
- 권한, 중복, 리소스 존재 여부와 상태 전이 검증
- Command 입력과 Result 출력

하나의 Domain 객체가 스스로 판단할 수 있는 규칙은 Service가 아니라 Domain 객체에 둔다.

### Repository

- `JdbcTemplate`을 이용한 SQL 실행
- DB Row와 Domain 객체 간 변환
- 저장, 수정, 삭제와 조회
- 조회 결과가 없을 때의 처리
- DB 예외를 애플리케이션에서 이해할 수 있는 형태로 변환

Repository에는 비즈니스 규칙을 작성하지 않는다.

### Domain

- Entity와 Value Object
- 불변 조건
- 상태 변경 규칙
- 비즈니스 판단
- Domain Policy

Domain 객체는 getter와 setter만 가진 데이터 묶음으로 만들지 않는다.

### Common

여러 도메인이 함께 사용하는 기술 공통 코드만 둔다.

- Spring 설정
- 전역 예외 처리
- 공통 성공·오류 응답과 민감한 검증값 제거 정책
- Stateless JWT 발급·검증과 인증 실패 응답
- Argument Resolver
- 요청 ID와 요청 메서드·경로·상태·소요 시간만 남기는 안전한 요청 로깅

특정 Domain의 규칙은 `common`에 두지 않는다. `common`을 이름을 정하기 어려운 코드의 임시 보관소로 사용하지 않는다.

`common/exception`의 예외 클래스 구조와 HTTP 변환 규칙은 [예외 컨벤션](../conventions/exception-convention.md)을 따른다.

### Member Client

- `GoogleOAuthGateway`는 Google authorization code 교환 경계다.
- `GoogleIdTokenVerifier`는 ID Token의 서명·issuer·audience·만료·nonce와 프로필 claim을 검증한다.
- 외부 Google 호출은 DB 트랜잭션 밖에서 수행한다.
- Google token과 자취선배 JWT를 저장하거나 로그로 남기지 않는다.

### Property

- `PropertyController`는 API-101부터 API-106까지의 HTTP 계약과 인증 회원 ID 전달을 담당한다.
- `ActiveChecklistController`는 API-401·402의 단계별 활성 체크리스트 설정·교체·해제 HTTP 계약을 담당한다.
- `PropertyCommandService`는 생성·기본 정보 변경과 구조화 메모·legacy 메모의 원자적 dual-write 트랜잭션을 담당한다. 메모 저장은 소유 매물 행을 잠그며 legacy 요청은 현재 일곱 구조화 필드를 보존한다.
- `PropertyQueryService`는 회원별 검색·페이징과 상세 Projection 조회를 담당한다.
- `PropertyQueryRepository`는 읽기 전용 방문 테이블과 구조화 사전 메모를 조인해 API-101·103의 최근 방문·집계·방문 수와 메모를 N+1 없이 반환한다. 구조화 행이 없으면 legacy 메모 컬럼을 읽기 fallback으로 사용한다.
- `PropertyPhotoController`와 `PropertyPhotoService`는 API-201부터 API-204까지의 검증·업로드·목록·인증 스트리밍·삭제 순서를 조율한다.
- `PropertyPhotoTransactionService`는 매물 행 잠금, 30장 제한과 사진 메타데이터 저장·삭제를 짧은 트랜잭션으로 처리한다.
- `PropertyDeletionService`는 사진 외부 객체 삭제와 `PropertyDeleteTransactionService`의 짧은 DB 삭제를 최대 3회 조율한다.
- `ActiveChecklistService`는 매물 행을 먼저 잠그고 `ChecklistReferenceQueryService`를 통해 체크리스트 소유권·단계를 확인한 뒤 연결을 upsert·멱등 해제한다. 이 유스케이스의 MySQL deadlock·lock timeout만 짧은 백오프로 최대 3회 시도한다.
- `PhotoStorage`는 비공개 객체의 `upload`, `open`, `deleteIfExists` 경계이며 `S3CompatiblePhotoStorage`가 S3 호환 SDK로 구현한다.
- `PropertyRepository`와 `PropertyPreVisitMemoRepository`의 모든 사용자 자원 변경 SQL, `PropertyQueryRepository`의 모든 조회 SQL은 `member_id` 조건을 포함한다. `PropertyPreVisitMemoRepository`는 구조화 메모 전체 upsert와 소유 조회를 담당한다.
- `SavePropertyMemoRequest`는 JSON 필드의 존재 여부를 추적해 v1.1 전체 표현과 v1.0 `content` 단독 표현을 구분하고 혼합·누락·null 요청을 계약된 오류로 변환한다.
- `PreVisitMemoField`, `PropertyPreVisitMemo`, `PropertyMemo`가 일곱 200 코드포인트 필드, 5,000 코드포인트 추가 메모, 저장 시각과 legacy 추가 메모 변경 시 구조화 값 보존 불변식을 보호한다.
- `PropertyName`, `Money`, `DiscoverySource`, `PropertyPhoto`가 나머지 매물 입력과 사진 메타데이터 불변식을 보호한다.
- `ActiveChecklist`와 `ActiveChecklistRepository`가 매물·단계별 현재 연결과 소유권 조건이 포함된 SQL을 담당한다.

### Common Page

- `PageQuery`는 page 0 이상, size 1~100 규칙을 검증한다.
- `PageResult`와 `PageResponse`는 목록 결과와 페이지 파생값을 서비스와 HTTP 경계에서 나눈다.

### Checklist

- `CheckItemController`와 `ChecklistPresetController`는 API-301·API-302의 제공 항목 검색과 읽기 전용 프리셋 조회를 담당한다.
- `ChecklistController`는 API-303부터 API-307까지의 회원 체크리스트 HTTP 계약과 인증 회원 ID 전달을 담당한다. `CreateChecklistRequest`와 `ReplaceChecklistRequest`는 JSON 필드 존재 여부를 추적해 v1.1 `items`와 deprecated v1.0 `checkItemIds`를 구분하고 혼합 표현을 거부한다.
- `CheckCatalogQueryService`는 활성 기준 항목 검색과 유형·단계 프리셋 조회를 담당한다.
- `ChecklistCommandService`는 PROVIDED·CUSTOM 생성, 전체 변경과 삭제의 소유권 잠금, 단계·중복·활성 상태·로컬 항목 소유 검증과 트랜잭션을 담당한다. CUSTOM이 있는 체크리스트의 legacy 전체 변경은 저장 전에 409로 차단한다.
- `ChecklistQueryService`는 회원·단계별 페이징 목록과 소유 상세 조회를 담당한다.
- `ChecklistReferenceQueryService`는 property가 활성 연결을 설정할 때 잠근 소유 체크리스트의 단계·이름·항목 수만 제공한다. 반대 방향 호출은 만들지 않는다.
- `ChecklistSnapshotSourceService`는 visit가 매물 행을 잠근 뒤 호출하며 현재 활성 체크리스트 루트를 ID 순으로 잠그고 이름·단계·PROVIDED·CUSTOM 정렬 항목과 nullable 출처를 스냅샷 원본으로 제공한다.
- `CheckItemRepository`와 `ChecklistPresetRepository`는 전역 PROVIDED만 담당한다. `ChecklistRepository`는 루트·항목 생성과 기존 로컬 ID를 보존하는 diff를, `ChecklistQueryRepository`는 `checklist_items`와 `check_items` LEFT JOIN으로 두 origin의 상세을 담당한다.
- `CheckStage`, `ChecklistPresetType`, `CheckItem`, `ChecklistName`, `Checklist`, `ChecklistItemOrigin`, `ChecklistItem`이 허용 값과 PROVIDED·CUSTOM 배타성, 로컬 ID, 질문·단계·순서 불변식을 보호한다.
- `ChecklistQueryRepository`는 활성 연결 테이블을 읽기 전용으로 집계해 API-303·305의 `assignedPropertyCount`를 한 조회 안에서 반환한다.

### Visit

- `VisitController`는 API-501부터 API-506까지 목록·시작·상세·상태 저장·완료·인라인 메모 저장의 HTTP 계약과 JWT 회원 ID 전달을 담당한다. `UpdateVisitItemRequest`는 v1.1 `expectedStatusVersion`과 deprecated `expectedVersion`의 존재 여부·값 일치를 검증하고 `UpdateVisitItemMemoRequest`는 메모 전용 명령으로 변환한다.
- `VisitQueryService`는 소유 매물 방문 목록과 소유 방문 상세를 조회하고 `VisitQueryRepository`는 origin·nullable 출처·질문 스냅샷과 상태·메모의 값·버전·저장 시각을 매핑한다. 상태 저장 시각만 부분 배포 호환 fallback을 사용하며 방문별·단계별 집계는 상태로 계산한다.
- `VisitCommandService.updateItemStatus`와 `updateItemMemo`는 각각 별도 쓰기 트랜잭션으로 매물 → 방문 → 채널별 조건부 UPDATE 순서를 조율한다. 상태 저장만 집계를 계산하고 메모 저장은 집계를 반환하거나 계산하지 않는다. 최초 완료를 포함한 세 쓰기는 MySQL deadlock·lock timeout만 짧은 백오프로 최대 3회 새 트랜잭션에서 시도한다.
- `VisitRepository`는 방문 루트 생성·소유 잠금·활동·최초 완료를, `VisitSnapshotRepository`는 단계와 PROVIDED·CUSTOM 항목 batch insert를 담당한다. `VisitCheckItemRepository.updateStatus`와 `updateMemo`는 회원·방문·항목과 각 채널 version 조건을 가진 서로 다른 UPDATE이며, 공개 조회 Projection도 상태와 메모로 나눠 충돌 코드를 섞지 않는다.
- `InlineMemo`는 null·200 Unicode 코드포인트·CR·LF 규칙과 공백 보존을 보호한다. `VisitCheckItem.changeStatus`와 `changeMemo`는 상대 채널 값을 그대로 보존하며 `Visit`, `VisitStatus`, `CheckStatus`, `VisitStageSnapshot`, `VisitSummary`가 나머지 상태·최초 완료 시각·스냅샷·집계 불변식을 보호한다.
- 방문 시작은 `PropertyAccessService`로 매물을 먼저 잠그고 `ChecklistSnapshotSourceService`로 원본 루트를 잠근 뒤 방문·전체 스냅샷·매물 활동을 한 트랜잭션에서 저장한다. 원본 변경·항목 삭제·체크리스트 삭제 뒤에도 저장한 origin·질문·안내·순서는 다시 라이브 조회하지 않는다.
- 완료 후 상태·인라인 메모 수정은 허용하고 완료 상태와 최초 완료 시각을 바꾸지 않는다. 최근 방문은 수정 시각이 아니라 시작 시각으로 정한다. 프론트엔드 debounce와 이동·완료 전 flush는 이 서버 패키지의 책임이 아니다.
- 방문 시작·상태 저장·메모 저장·최초 완료는 커밋 뒤 식별자와 채널별 version만 구조화 로그로 관찰한다. 메모 본문·길이·해시는 기록하지 않는다. 서로 다른 완료 매물이 2개 이상이면 반복 사용 달성을 중복 허용 로그로 남기며 관찰 실패가 완료 응답을 바꾸지 않는다.

---

세부 작성 규칙은 [백엔드 코드 컨벤션](../conventions/backend-code-convention.md), 선택 근거는 [ADR-0003](../adr/0003-select-database-and-persistence.md)을 따른다.
