-- 사용자 체크리스트가 시스템 항목 변경·비활성화의 영향을 받지 않도록 항목 정보를 스냅샷한다.
ALTER TABLE user_checklist_items
    ADD COLUMN stage VARCHAR(30) NULL AFTER system_check_item_id,
    ADD COLUMN item_type VARCHAR(20) NULL AFTER stage,
    ADD COLUMN question VARCHAR(200) NULL AFTER item_type;

UPDATE user_checklist_items u
JOIN system_check_items s ON s.id = u.system_check_item_id
SET u.stage = s.stage,
    u.item_type = s.item_type,
    u.question = s.question;

ALTER TABLE user_checklist_items
    MODIFY COLUMN stage VARCHAR(30) NOT NULL,
    MODIFY COLUMN item_type VARCHAR(20) NOT NULL,
    MODIFY COLUMN question VARCHAR(200) NOT NULL;
