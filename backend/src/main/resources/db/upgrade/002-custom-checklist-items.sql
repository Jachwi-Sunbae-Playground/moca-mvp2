-- 기존 체크리스트 스냅샷을 유지하면서 사용자 직접 문항을 허용하고 새 제공 문항으로 전환한다.
-- 이전 제공 문항은 삭제하지 않고 비활성화해 기존 FK와 질문 스냅샷을 보존한다.

ALTER TABLE user_checklist_items
    MODIFY system_check_item_id BIGINT UNSIGNED NULL;

ALTER TABLE property_checklist_items
    MODIFY system_check_item_id BIGINT UNSIGNED NULL;

UPDATE system_check_items
SET deleted_at = COALESCE(deleted_at, CURRENT_TIMESTAMP(6))
WHERE id BETWEEN 1 AND 18;

INSERT INTO system_check_items (id, stage, item_type, question, deleted_at)
VALUES
    (101, 'ONLINE_PHONE', 'CORE', '보증금과 월세, 관리비는 어떤가요?', NULL),
    (102, 'ONLINE_PHONE', 'CORE', '관리비에 어떤 항목이 포함되는가? (인터넷, 수도세, 전기세, 가스비)', NULL),
    (103, 'ONLINE_PHONE', 'CORE', '입주 가능일을 확인했는가?', NULL),
    (104, 'ONLINE_PHONE', 'CORE', '위치와 통학 및 통근 시간이 괜찮은가? (대중교통 편의성)', NULL),
    (105, 'ONLINE_PHONE', 'CORE', '옵션에는 무엇이 포함되어 있는가?', NULL),
    (106, 'ONLINE_PHONE', 'CORE', '전입신고가 가능한가?', NULL),
    (107, 'ONLINE_PHONE', 'OPTIONAL', '주변에 편의 시설은 어떤가? (편의점, 마트 등등)', NULL),
    (108, 'ONLINE_PHONE', 'OPTIONAL', '주변에 소음 시설은 어떤가? (유흥주점, 배달전문점, 대로변)', NULL),
    (109, 'ONLINE_PHONE', 'OPTIONAL', '밤 시간 귀가 동선이 안전한지, 골목 조명과 유동인구는 어떤가?', NULL),
    (110, 'ONLINE_PHONE', 'OPTIONAL', '오르막이나 언덕은 어떤가?', NULL),
    (111, 'ONLINE_PHONE', 'OPTIONAL', '가계약금이나 예약금은 어떤가?', NULL),
    (112, 'ONLINE_PHONE', 'OPTIONAL', '정부 주거 지원금은 어떠한가?', NULL),
    (113, 'ON_SITE', 'CORE', '가구 배치시 방구조가 어떤가?', NULL),
    (114, 'ON_SITE', 'CORE', '햇빛이 들어오는건 어떤가?', NULL),
    (115, 'ON_SITE', 'CORE', '방충망/방범창 이상 없고 괜찮은가?', NULL),
    (116, 'ON_SITE', 'CORE', '환기는 어떤가?', NULL),
    (117, 'ON_SITE', 'CORE', '싱크대/세면대/샤워기 물 잘 나오는가?', NULL),
    (118, 'ON_SITE', 'CORE', '변기 물 잘 내려가는가?', NULL),
    (119, 'ON_SITE', 'CORE', '옵션 가구 종류 확인했는가?', NULL),
    (120, 'ON_SITE', 'CORE', '쓰레기 배출 장소 방법은 어떤가?', NULL),
    (121, 'ON_SITE', 'OPTIONAL', '어플 사진과 실제와 차이는 어떤가?', NULL),
    (122, 'ON_SITE', 'OPTIONAL', 'CCTV는 어떤가?', NULL),
    (123, 'ON_SITE', 'OPTIONAL', '싱크대/화장실 배수구 잘 내려가는가?', NULL),
    (124, 'ON_SITE', 'OPTIONAL', '온수 잘 나오는지, 난방 잘 되는가?', NULL),
    (125, 'ON_SITE', 'OPTIONAL', '화장실 내부에 창문 있는가?', NULL),
    (126, 'ON_SITE', 'OPTIONAL', '세면대 있는가?', NULL),
    (127, 'ON_SITE', 'OPTIONAL', '배수구 냄새 괜찮은가?', NULL),
    (128, 'ON_SITE', 'OPTIONAL', '샤워 여유 공간은 괜찮은가?', NULL),
    (129, 'ON_SITE', 'OPTIONAL', '에어컨/냉장고 작동 괜찮은가?', NULL),
    (130, 'ON_SITE', 'OPTIONAL', '화구 종류 체크했는가?', NULL),
    (131, 'ON_SITE', 'OPTIONAL', '옵션 가구 필요 없다면 치워줄 수 있는지 확인했는가?', NULL),
    (132, 'ON_SITE', 'OPTIONAL', '수납 공간 충분한가?', NULL),
    (133, 'ON_SITE', 'OPTIONAL', '곰팡이, 결로, 누수 흔적은 어떤가?', NULL),
    (134, 'ON_SITE', 'OPTIONAL', '벌레 흔적은 어땠는가?', NULL),
    (135, 'ON_SITE', 'OPTIONAL', '콘센트 개수 충분했는가?', NULL),
    (136, 'ON_SITE', 'OPTIONAL', '방음은 어떤가?', NULL),
    (137, 'ON_SITE', 'OPTIONAL', '최근 방이나 건물에 수리한 내역은 어떤가?', NULL),
    (138, 'ON_SITE', 'OPTIONAL', '주차 공간은 어떤가?', NULL),
    (139, 'ON_SITE', 'OPTIONAL', '인터넷 설치가 되어있는가?', NULL),
    (140, 'PRE_CONTRACT', 'CORE', '신분증과 도장을 준비했는가?', NULL),
    (141, 'PRE_CONTRACT', 'CORE', '보증금이 준비 되었는가?', NULL),
    (142, 'PRE_CONTRACT', 'CORE', '보증금, 월세, 관리비를 확인했는가?', NULL),
    (143, 'PRE_CONTRACT', 'CORE', '계약 시작, 종료, 입주, 퇴거일과 같은 계약 기간을 확인했는가?', NULL),
    (144, 'PRE_CONTRACT', 'CORE', '관리비(전기, 수도, 인터넷 등 포함 여부) 및 추가 별도 공과금을 확인했는가?', NULL),
    (145, 'PRE_CONTRACT', 'CORE', '파손 및 손해배상 조건을 확인했는가?', NULL),
    (146, 'PRE_CONTRACT', 'CORE', '전입신고와 확정일자 발급이 가능한지 확인했는가?', NULL),
    (147, 'PRE_CONTRACT', 'OPTIONAL', '퇴거 시 청소비 및 수리비 조건을 확인했는가?', NULL),
    (148, 'PRE_CONTRACT', 'OPTIONAL', '계약 해지 및 위약금 규정 확인을 했는가?', NULL),
    (149, 'PRE_CONTRACT', 'OPTIONAL', '집주인(임대인)의 신분이 등기부등본 소유자와 일치하는지 확인했는가?', NULL),
    (150, 'PRE_CONTRACT', 'OPTIONAL', '소유자와 계약자가 동일한지 확인했는가?', NULL),
    (151, 'PRE_CONTRACT', 'OPTIONAL', '대리인 계약이면 위임장, 인감증명서, 신분증 사본 등 원본 증빙을 확인했는가?', NULL),
    (152, 'PRE_CONTRACT', 'OPTIONAL', '근저당권, 전세권, 가압류, 압류가 있는지 확인했는가?', NULL),
    (153, 'PRE_CONTRACT', 'OPTIONAL', '가처분, 가압류 등 분쟁 기록이 없는지 확인했는가?', NULL) AS new
ON DUPLICATE KEY UPDATE
    stage = new.stage,
    item_type = new.item_type,
    question = new.question,
    deleted_at = NULL;
