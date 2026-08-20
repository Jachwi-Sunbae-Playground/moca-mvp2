-- 매물 메모가 시스템 템플릿 변경·소프트 삭제의 영향을 받지 않도록 당시 표시 정보를 보존한다.
ALTER TABLE property_memo_items
    ADD COLUMN label VARCHAR(30) NOT NULL DEFAULT '' AFTER system_meno_id,
    ADD COLUMN display_order SMALLINT UNSIGNED NOT NULL DEFAULT 1 AFTER label;
