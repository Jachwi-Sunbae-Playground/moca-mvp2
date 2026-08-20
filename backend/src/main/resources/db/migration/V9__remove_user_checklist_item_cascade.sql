-- 사용자 체크리스트 아이템 삭제를 서비스 트랜잭션에서 명시적으로 처리한다.
ALTER TABLE user_checklist_items
    DROP FOREIGN KEY fk_user_checklist_items_checklist;

ALTER TABLE user_checklist_items
    ADD CONSTRAINT fk_user_checklist_items_checklist
        FOREIGN KEY (user_checklist_id) REFERENCES user_checklists (id);
