-- 매물 종속 데이터는 PropertyService에서 명시적으로 삭제한다.
ALTER TABLE main_property_photos
    DROP FOREIGN KEY fk_main_property_photos_property,
    DROP FOREIGN KEY fk_main_property_photos_photo;

ALTER TABLE main_property_photos
    ADD CONSTRAINT fk_main_property_photos_property
        FOREIGN KEY (property_id) REFERENCES properties (id),
    ADD CONSTRAINT fk_main_property_photos_photo
        FOREIGN KEY (property_photos_id) REFERENCES property_photos (id);

ALTER TABLE property_memos
    DROP FOREIGN KEY fk_property_memos_property;

ALTER TABLE property_memos
    ADD CONSTRAINT fk_property_memos_property
        FOREIGN KEY (property_id) REFERENCES properties (id);

ALTER TABLE property_memo_items
    DROP FOREIGN KEY fk_property_memo_items_memo;

ALTER TABLE property_memo_items
    ADD CONSTRAINT fk_property_memo_items_memo
        FOREIGN KEY (property_memo_id) REFERENCES property_memos (id);

ALTER TABLE property_checklists
    DROP FOREIGN KEY fk_property_checklists_property;

ALTER TABLE property_checklists
    ADD CONSTRAINT fk_property_checklists_property
        FOREIGN KEY (property_id) REFERENCES properties (id);

ALTER TABLE property_checklist_items
    DROP FOREIGN KEY fk_property_checklist_items_checklist;

ALTER TABLE property_checklist_items
    ADD CONSTRAINT fk_property_checklist_items_checklist
        FOREIGN KEY (property_checklist_id) REFERENCES property_checklists (id);
