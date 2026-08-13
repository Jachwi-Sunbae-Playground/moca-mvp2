CREATE TABLE property_pre_visit_memos
(
    property_id                  BIGINT        NOT NULL PRIMARY KEY,
    member_id                    BIGINT        NOT NULL,
    viewing_schedule            VARCHAR(200)  NOT NULL DEFAULT '',
    move_in_availability        VARCHAR(200)  NOT NULL DEFAULT '',
    provisional_deposit         VARCHAR(200)  NOT NULL DEFAULT '',
    room_options                VARCHAR(200)  NOT NULL DEFAULT '',
    maintenance_and_utilities   VARCHAR(200)  NOT NULL DEFAULT '',
    commute_time                VARCHAR(200)  NOT NULL DEFAULT '',
    government_support          VARCHAR(200)  NOT NULL DEFAULT '',
    additional_memo             VARCHAR(5000) NOT NULL DEFAULT '',
    saved_at                    DATETIME(6)    NOT NULL,
    created_at                  DATETIME(6)    NOT NULL,
    updated_at                  DATETIME(6)    NOT NULL,
    CONSTRAINT fk_pre_visit_memos_property_owner
        FOREIGN KEY (property_id, member_id) REFERENCES properties (id, member_id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

ALTER TABLE checklist_items
    DROP FOREIGN KEY fk_checklist_items_check_item_stage;

ALTER TABLE checklist_items
    DROP PRIMARY KEY,
    ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT FIRST,
    ADD COLUMN origin VARCHAR(20) NOT NULL DEFAULT 'PROVIDED' AFTER checklist_id,
    MODIFY COLUMN check_item_id BIGINT NULL,
    ADD COLUMN custom_question VARCHAR(200) NULL AFTER check_item_id,
    ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ADD PRIMARY KEY (id),
    ADD CONSTRAINT uk_checklist_items_id_checklist UNIQUE (id, checklist_id),
    ADD CONSTRAINT uk_checklist_items_provided UNIQUE (checklist_id, check_item_id),
    ADD CONSTRAINT fk_checklist_items_check_item_stage
        FOREIGN KEY (check_item_id, stage) REFERENCES check_items (id, stage) ON DELETE RESTRICT;

ALTER TABLE visit_check_items
    DROP FOREIGN KEY fk_visit_items_source_stage;

ALTER TABLE visit_check_items
    ADD COLUMN origin VARCHAR(20) NOT NULL DEFAULT 'PROVIDED' AFTER stage,
    ADD COLUMN source_checklist_item_id BIGINT NULL AFTER origin,
    MODIFY COLUMN source_check_item_id BIGINT NULL,
    ADD COLUMN status_saved_at DATETIME(6) NULL AFTER version,
    ADD COLUMN inline_memo VARCHAR(200) NOT NULL DEFAULT '' AFTER status_saved_at,
    ADD COLUMN memo_version BIGINT NOT NULL DEFAULT 0 AFTER inline_memo,
    ADD COLUMN memo_updated_at DATETIME(6) NULL AFTER memo_version,
    ADD CONSTRAINT uk_visit_items_source_checklist_item
        UNIQUE (visit_stage_snapshot_id, source_checklist_item_id),
    ADD CONSTRAINT fk_visit_items_source_checklist_item
        FOREIGN KEY (source_checklist_item_id) REFERENCES checklist_items (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_visit_items_source_stage
        FOREIGN KEY (source_check_item_id, stage) REFERENCES check_items (id, stage) ON DELETE RESTRICT;
