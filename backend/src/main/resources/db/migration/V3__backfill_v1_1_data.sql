INSERT INTO property_pre_visit_memos (
    property_id,
    member_id,
    viewing_schedule,
    move_in_availability,
    provisional_deposit,
    room_options,
    maintenance_and_utilities,
    commute_time,
    government_support,
    additional_memo,
    saved_at,
    created_at,
    updated_at
)
SELECT id,
       member_id,
       '',
       '',
       '',
       '',
       '',
       '',
       '',
       memo,
       COALESCE(memo_updated_at, updated_at),
       created_at,
       COALESCE(memo_updated_at, updated_at)
FROM properties;

UPDATE checklist_items
SET origin = 'PROVIDED'
WHERE origin <> 'PROVIDED';

UPDATE visit_check_items item
JOIN visit_stage_snapshots snapshot
  ON snapshot.id = item.visit_stage_snapshot_id
JOIN checklist_items source_item
  ON source_item.checklist_id = snapshot.source_checklist_id
 AND source_item.check_item_id = item.source_check_item_id
SET item.source_checklist_item_id = source_item.id
WHERE item.source_checklist_item_id IS NULL;

UPDATE visit_check_items
SET origin = 'PROVIDED',
    status_saved_at = updated_at,
    inline_memo = '',
    memo_version = 0,
    memo_updated_at = NULL;

UPDATE checklist_presets
SET is_active = FALSE,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE preset_type = 'GOSHIWON';

UPDATE check_items
SET question = CASE id
    WHEN 101 THEN '보일러 상태는 괜찮은가?'
    WHEN 102 THEN '채광과 창문 방향은 내 기준에 괜찮은가?'
    WHEN 103 THEN '환기 상태는 괜찮은가?'
    WHEN 104 THEN '사생활 보호 수준은 괜찮은가?'
    WHEN 105 THEN '곰팡이 흔적은 걱정되지 않는가?'
    WHEN 106 THEN '결로 흔적은 걱정되지 않는가?'
    WHEN 107 THEN '벽이나 천장의 누수 흔적은 걱정되지 않는가?'
    WHEN 108 THEN '싱크대·세면대 아래 누수 흔적은 걱정되지 않는가?'
    WHEN 109 THEN '악취나 벌레 흔적은 걱정되지 않는가?'
    WHEN 110 THEN '수압은 괜찮은가?'
    WHEN 111 THEN '온수 상태는 괜찮은가?'
    WHEN 112 THEN '화장실 배수 상태는 괜찮은가?'
    WHEN 113 THEN '화장실 환기 상태는 괜찮은가?'
    WHEN 114 THEN '배수구 냄새는 걱정되지 않는가?'
    WHEN 115 THEN '외부 소음 수준은 괜찮은가?'
    WHEN 116 THEN '층간·벽간 소음 수준은 괜찮은가?'
    WHEN 117 THEN '휴대전화 수신 상태는 괜찮은가?'
    WHEN 118 THEN '콘센트 위치와 개수는 충분한가?'
    WHEN 119 THEN '난방 방식과 예상 비용은 괜찮은가?'
    WHEN 120 THEN '전기·수도·가스 계량 방식은 괜찮은가?'
    WHEN 121 THEN '필요한 가구를 배치할 공간은 충분한가?'
    WHEN 122 THEN '생활 동선은 편리한가?'
    WHEN 123 THEN '수납공간은 충분한가?'
    WHEN 124 THEN '문 폭은 이삿짐을 옮기기에 충분한가?'
    WHEN 125 THEN '에어컨 상태는 괜찮은가?'
    WHEN 126 THEN '냉장고 상태는 괜찮은가?'
    WHEN 127 THEN '세탁기 상태는 괜찮은가?'
    WHEN 128 THEN '파손·오염·옵션 상태를 남길 사진은 충분한가?'
    WHEN 129 THEN '공동현관 잠금장치는 괜찮은가?'
    WHEN 130 THEN 'CCTV 설치 위치와 범위는 괜찮은가?'
    WHEN 131 THEN '현관문·창문 잠금장치 상태는 괜찮은가?'
    WHEN 132 THEN '복도·계단·엘리베이터 관리 상태는 괜찮은가?'
    WHEN 133 THEN '소화기·화재감지기·비상구 상태는 괜찮은가?'
    WHEN 134 THEN '역·정류장에서 집까지 이동하기 괜찮은가?'
    WHEN 135 THEN '언덕이나 불편한 이동 구간은 감수할 수 있는가?'
    WHEN 136 THEN '야간 귀가 동선은 괜찮은가?'
    WHEN 137 THEN '쓰레기·재활용·음식물 배출 방식은 편리한가?'
    WHEN 138 THEN '편의점·마트·병원·약국 등 생활시설 접근성은 괜찮은가?'
    WHEN 201 THEN '보증금·월세·관리비 조건은 예산에 괜찮은가?'
    WHEN 202 THEN '관리비 포함 항목은 납득할 만한가?'
    WHEN 203 THEN '입주 가능일은 내 일정에 괜찮은가?'
    WHEN 204 THEN '주변 시세와 비교한 가격은 괜찮은가?'
    WHEN 205 THEN '위치와 통학·통근 시간은 괜찮은가?'
    WHEN 206 THEN '매물 사진과 정보는 판단하기에 충분한가?'
    WHEN 207 THEN '현재 계약 가능한 매물인가?'
    WHEN 208 THEN '광고 조건과 실제 보증금·월세 조건은 일치하는가?'
    WHEN 209 THEN '관리비와 별도 공과금 조건은 괜찮은가?'
    WHEN 210 THEN '포함 옵션 구성은 괜찮은가?'
    WHEN 211 THEN '중개사가 안내한 입주 가능일은 내 일정에 괜찮은가?'
    WHEN 212 THEN '전입신고 가능 여부는 내 조건에 괜찮은가?'
    WHEN 213 THEN '필요한 대출을 이용할 수 있는 매물인가?'
    WHEN 214 THEN '보증보험 가입 가능 여부는 내 조건에 괜찮은가?'
    WHEN 215 THEN '방문 전 요구하는 가계약금·예약금 조건은 괜찮은가?'
    WHEN 301 THEN '총주거비는 예산에 괜찮은가?'
    WHEN 302 THEN '건축물대장 주소·용도·위반 여부는 괜찮은가?'
    WHEN 303 THEN '등기사항증명서의 소유자 정보는 계약 상대와 일치하는가?'
    WHEN 304 THEN '근저당·압류 등 권리관계는 감수할 수 있는가?'
    WHEN 305 THEN '보증금은 주변 시세와 비교해 괜찮은가?'
    WHEN 306 THEN '필요한 대출을 받을 수 있는 계약인가?'
    WHEN 307 THEN '보증보험에 가입할 수 있는 계약인가?'
    WHEN 308 THEN '수리 내용과 비용 부담 조건은 괜찮은가?'
    WHEN 309 THEN '중요한 약속은 확인할 수 있는 기록으로 남아 있는가?'
    WHEN 310 THEN '계약 당일 최신 등기사항증명서의 권리관계는 괜찮은가?'
    WHEN 311 THEN '계약 상대방은 등기상 소유자와 일치하는가?'
    WHEN 312 THEN '송금 계좌 명의는 소유자와 일치하는가?'
    WHEN 313 THEN '보증금·월세·납부일·계약기간 조건은 명확하고 괜찮은가?'
    WHEN 314 THEN '관리비 포함·별도 항목은 명확하고 괜찮은가?'
    WHEN 315 THEN '옵션·파손·수리 약속은 계약서나 특약에 충분히 남아 있는가?'
    WHEN 316 THEN '대출·보증보험 불가 시 계약금 반환 조건은 괜찮은가?'
    WHEN 317 THEN '잔금 전 신규 권리 설정 금지 특약은 충분한가?'
    WHEN 318 THEN '계약서의 빈칸이나 이해되지 않는 내용은 없는가?'
    WHEN 319 THEN '계약서·확인설명서·영수증 등 필요한 문서를 충분히 받았는가?'
    ELSE question
END,
updated_at = CURRENT_TIMESTAMP(6)
WHERE id IN (
    101, 102, 103, 104, 105, 106, 107, 108, 109, 110,
    111, 112, 113, 114, 115, 116, 117, 118, 119, 120,
    121, 122, 123, 124, 125, 126, 127, 128, 129, 130,
    131, 132, 133, 134, 135, 136, 137, 138,
    201, 202, 203, 204, 205, 206, 207, 208, 209, 210,
    211, 212, 213, 214, 215,
    301, 302, 303, 304, 305, 306, 307, 308, 309, 310,
    311, 312, 313, 314, 315, 316, 317, 318, 319
);
