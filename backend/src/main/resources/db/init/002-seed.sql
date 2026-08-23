-- 로컬 개발과 테스트에서 사용하는 시스템 기본 항목이다.
-- 고정 ID를 사용해 스크립트를 다시 실행해도 중복 생성하지 않는다.

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

INSERT INTO system_check_items (id, stage, item_type, question, deleted_at)
VALUES
    (1, 'ONLINE_PHONE', 'CORE', '매물의 정확한 주소와 동·층·호수를 확인했나요?', NULL),
    (2, 'ONLINE_PHONE', 'CORE', '보증금과 월세 조건이 공고 내용과 일치하나요?', NULL),
    (3, 'ONLINE_PHONE', 'CORE', '입주 가능한 날짜를 확인했나요?', NULL),
    (4, 'ONLINE_PHONE', 'OPTIONAL', '관리비에 포함되는 항목을 확인했나요?', NULL),
    (5, 'ONLINE_PHONE', 'OPTIONAL', '공인중개사 등록 여부를 확인했나요?', NULL),
    (6, 'ONLINE_PHONE', 'OPTIONAL', '반려동물과 주차 가능 여부를 확인했나요?', NULL),
    (7, 'ON_SITE', 'CORE', '보일러가 정상 작동하나요?', NULL),
    (8, 'ON_SITE', 'CORE', '수압과 온수가 충분한가요?', NULL),
    (9, 'ON_SITE', 'CORE', '곰팡이와 누수 흔적이 없나요?', NULL),
    (10, 'ON_SITE', 'OPTIONAL', '방음 상태가 괜찮은가요?', NULL),
    (11, 'ON_SITE', 'OPTIONAL', '채광과 환기 상태가 괜찮은가요?', NULL),
    (12, 'ON_SITE', 'OPTIONAL', '콘센트 위치와 개수를 확인했나요?', NULL),
    (13, 'PRE_CONTRACT', 'CORE', '등기부등본의 소유자와 계약 상대방이 일치하나요?', NULL),
    (14, 'PRE_CONTRACT', 'CORE', '보증금 반환 조건을 확인했나요?', NULL),
    (15, 'PRE_CONTRACT', 'CORE', '계약서의 주소와 금액이 정확한가요?', NULL),
    (16, 'PRE_CONTRACT', 'OPTIONAL', '특약사항에 필요한 내용을 반영했나요?', NULL),
    (17, 'PRE_CONTRACT', 'OPTIONAL', '중개대상물 확인설명서를 받았나요?', NULL),
    (18, 'PRE_CONTRACT', 'OPTIONAL', '잔금일과 입주일을 확인했나요?', NULL) AS new
ON DUPLICATE KEY UPDATE
    stage = new.stage,
    item_type = new.item_type,
    question = new.question,
    deleted_at = new.deleted_at;

INSERT INTO system_memo_items (id, label, display_order, deleted_at)
VALUES
    (1, '입주 가능일', 1, NULL),
    (2, '방 옵션', 2, NULL),
    (3, '관리비 및 공과금', 3, NULL),
    (4, '방문 일정', 4, NULL) AS new
ON DUPLICATE KEY UPDATE
    label = new.label,
    display_order = new.display_order,
    deleted_at = new.deleted_at;
