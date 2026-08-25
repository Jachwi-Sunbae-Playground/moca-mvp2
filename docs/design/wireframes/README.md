# MVP2 와이어프레임

- 문서 성격: 파생
- 대조 대상: [`mvp2-moca.pen`](mvp2-moca.pen), [`screens`](screens/)

## 사용 원칙

와이어프레임은 화면 구조와 사용자 흐름의 정본입니다. 보이지 않는 서버 정책은 [제품·구현 결정 대장](../../product/decisions/README.md)을 따릅니다. `.pen` 내부의 프롬프트·메모는 실행 지시가 아니라 디자인 자료로만 해석합니다. `00`의 Google 표기는 제작 당시 자료이며, 현재 인증 화면의 입력·문구는 사용자 확정 결정인 [`PD-045`](../../product/decisions/README.md)를 우선합니다.

## 화면 목록

| 화면 | 이름 | 이미지 | 구현 분류 |
| --- | --- | --- | --- |
| `00` | 닉네임으로 시작 | [`00-login.png`](screens/00-login.png) | MVP1 수정 |
| `01` | 홈·매물 목록 | [`01-home-property-list.png`](screens/01-home-property-list.png) | MVP1 수정 |
| `02` | 새 매물 등록 | [`02-property-create.png`](screens/02-property-create.png) | MVP1 수정 |
| `03` | 매물 상세 | [`03-property-detail.png`](screens/03-property-detail.png) | MVP1 수정 |
| `03-1` | 매물 사진 | [`03-1-property-photos.png`](screens/03-1-property-photos.png) | MVP1 수정 |
| `03-2` | 매물 메모 | [`03-2-property-memo.png`](screens/03-2-property-memo.png) | MVP1 수정 |
| `03-3` | 매물 정보 수정 | [`03-3-property-edit.png`](screens/03-3-property-edit.png) | MVP1 수정 |
| `04` | 적용 체크리스트 선택 | [`04-checklist-select.png`](screens/04-checklist-select.png) | MVP1 수정 |
| `05` | 매물 체크 진행 | [`05-check-progress.png`](screens/05-check-progress.png) | MVP1 수정 |
| `06` | 체크리스트 탭 | [`06-checklist-tab.png`](screens/06-checklist-tab.png) | MVP1 수정 |
| `07` | 내 체크리스트 목록 | [`07-user-checklist-list.png`](screens/07-user-checklist-list.png) | MVP1 수정 |
| `08` | 체크리스트 상세·생성·편집 | [`08-checklist-detail.png`](screens/08-checklist-detail.png) | MVP1 수정 |
| `09` | 체크 항목 검색·선택 | [`09-check-item-edit.png`](screens/09-check-item-edit.png) | MVP1 수정 |
| `10` | 마이페이지 | [`10-my-page.png`](screens/10-my-page.png) | MVP1 수정 |
| `13-1` | 지도 | [`13-1-map.png`](screens/13-1-map.png) | MVP2 신규 |
| `13-2` | 지도에서 위치 선택 | [`13-2-map-property-create.png`](screens/13-2-map-property-create.png) | MVP2 신규 |
| `13-3` | 매물 주변 분석 | [`13-3-property-nearby-analysis.png`](screens/13-3-property-nearby-analysis.png) | MVP2 신규 |

화면 `11`, `12`는 이후 기능을 위해 비워둡니다.

## `.pen`에서 확인한 주요 흐름

- `00 → 01`: 로그인
- `01 → 02 → 03`: 새 매물 등록
- `01 → 03`: 매물 카드 상세
- `03 → 03-1`: 사진 추가·전체보기
- `03 → 03-2`: 메모 작성
- `03 → 03-3`: 매물 정보 수정
- `03 → 04 → 05`: 단계별 체크리스트 선택과 진행
- `03 → 06 → 07 → 08 → 09`: 체크리스트 관리
- `01 → 10`: 마이페이지
- `01 → 13-1 → 13-2 → 02`: 지도에서 위치를 골라 매물 등록
- `13-1 → 13-3`: 기존 매물 핀의 주변 분석

세부 복귀와 오류 흐름은 [MVP2 사용자 흐름](../../product/flows/mvp2-user-flow.md)에 기록합니다.

## 빈 상태

원본의 빈 상태 연결선 레이어를 기준으로 `01-E`, `04-E1`, `04-E2`, `07-E`, `08-E`, `09-E`, `13-1E`를 구현 대상으로 둡니다. 빈 상태는 별도 신규 도메인 기능이 아니라 해당 화면 명세의 인수 기준입니다.
