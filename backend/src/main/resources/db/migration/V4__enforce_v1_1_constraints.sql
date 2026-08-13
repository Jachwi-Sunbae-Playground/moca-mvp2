ALTER TABLE checklist_items
    ADD CONSTRAINT ck_checklist_items_origin
        CHECK (origin IN ('PROVIDED', 'CUSTOM')),
    ADD CONSTRAINT ck_checklist_items_source
        CHECK (
            (origin = 'PROVIDED' AND check_item_id IS NOT NULL AND custom_question IS NULL)
            OR
            (origin = 'CUSTOM' AND check_item_id IS NULL
                AND CHAR_LENGTH(TRIM(custom_question)) BETWEEN 1 AND 200)
        );

ALTER TABLE visit_check_items
    ADD CONSTRAINT ck_visit_items_origin
        CHECK (origin IN ('PROVIDED', 'CUSTOM')),
    ADD CONSTRAINT ck_visit_items_source
        CHECK (
            (origin = 'PROVIDED' AND source_check_item_id IS NOT NULL)
            OR
            (origin = 'CUSTOM' AND source_check_item_id IS NULL)
        ),
    ADD CONSTRAINT ck_visit_items_memo_version
        CHECK (memo_version >= 0),
    ADD CONSTRAINT ck_visit_items_inline_memo
        CHECK (
            CHAR_LENGTH(inline_memo) <= 200
            AND HEX(inline_memo) NOT LIKE '%0A%'
            AND HEX(inline_memo) NOT LIKE '%0D%'
        );
