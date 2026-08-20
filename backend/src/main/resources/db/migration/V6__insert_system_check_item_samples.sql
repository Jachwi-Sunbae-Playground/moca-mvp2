-- 로컬 개발과 체크리스트 화면 검증을 위한 시스템 체크 항목 샘플
INSERT INTO system_check_items (stage, item_type, question, deleted_at)
VALUES
    ('ONLINE_PHONE', 'CORE', '매물의 정확한 주소와 동·층·호수를 확인했나요?', NULL),
    ('ONLINE_PHONE', 'CORE', '보증금과 월세 조건이 공고 내용과 일치하나요?', NULL),
    ('ONLINE_PHONE', 'OPTIONAL', '관리비에 포함되는 항목을 확인했나요?', NULL),
    ('ONLINE_PHONE', 'OPTIONAL', '계약 가능한 입주일을 확인했나요?', NULL),
    ('ON_SITE', 'CORE', '창문과 벽에 결로나 곰팡이가 있나요?', NULL),
    ('ON_SITE', 'CORE', '싱크대와 욕실의 수압이 충분한가요?', NULL),
    ('ON_SITE', 'OPTIONAL', '채광과 환기 상태가 괜찮은가요?', NULL),
    ('ON_SITE', 'OPTIONAL', '콘센트 위치와 개수를 확인했나요?', NULL),
    ('PRE_CONTRACT', 'CORE', '등기부등본의 소유자와 계약 상대방이 일치하나요?', NULL),
    ('PRE_CONTRACT', 'OPTIONAL', '특약사항에 필요한 내용을 반영했나요?', NULL);

-- 와이어프레임의 매물 기본 메모 템플릿
INSERT INTO system_memo_items (label, display_order, deleted_at)
VALUES
    ('집 주소', 1, NULL),
    ('입주 가능일', 2, NULL),
    ('가계약금', 3, NULL),
    ('방 옵션', 4, NULL),
    ('관리비 및 공과금', 5, NULL),
    ('통학 통근 시간', 6, NULL);
