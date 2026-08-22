-- 로컬 개발과 테스트에서 사용하는 시스템 기본 항목이다.
-- 고정 ID를 사용해 스크립트를 다시 실행해도 중복 생성하지 않는다.

INSERT IGNORE INTO system_check_items (id, stage, item_type, question, deleted_at)
VALUES
    (1, 'ONLINE_PHONE', 'CORE', '매물의 정확한 주소와 동·층·호수를 확인했나요?', NULL),
    (2, 'ONLINE_PHONE', 'CORE', '보증금과 월세 조건이 공고 내용과 일치하나요?', NULL),
    (3, 'ONLINE_PHONE', 'OPTIONAL', '관리비에 포함되는 항목을 확인했나요?', NULL),
    (4, 'ONLINE_PHONE', 'OPTIONAL', '계약 가능한 입주일을 확인했나요?', NULL),
    (5, 'ON_SITE', 'CORE', '창문과 벽에 결로나 곰팡이가 있나요?', NULL),
    (6, 'ON_SITE', 'CORE', '싱크대와 욕실의 수압이 충분한가요?', NULL),
    (7, 'ON_SITE', 'OPTIONAL', '채광과 환기 상태가 괜찮은가요?', NULL),
    (8, 'ON_SITE', 'OPTIONAL', '콘센트 위치와 개수를 확인했나요?', NULL),
    (9, 'PRE_CONTRACT', 'CORE', '등기부등본의 소유자와 계약 상대방이 일치하나요?', NULL),
    (10, 'PRE_CONTRACT', 'OPTIONAL', '특약사항에 필요한 내용을 반영했나요?', NULL);

INSERT IGNORE INTO system_memo_items (id, label, display_order, deleted_at)
VALUES
    (1, '집 주소', 1, NULL),
    (2, '입주 가능일', 2, NULL),
    (3, '가계약금', 3, NULL),
    (4, '방 옵션', 4, NULL),
    (5, '관리비 및 공과금', 5, NULL),
    (6, '통학 통근 시간', 6, NULL);
