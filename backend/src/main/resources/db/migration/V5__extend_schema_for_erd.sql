-- 첨부 ERD를 기존 v1.x 데이터와 호환되도록 확장한다.
-- 기존 테이블과 데이터는 삭제하지 않는다. 레거시 컬럼은 후속 정리 마이그레이션에서 제거한다.

ALTER TABLE members
    ADD COLUMN name VARCHAR(100) NULL AFTER email;

UPDATE members
SET name = display_name
WHERE name IS NULL;

ALTER TABLE members
    MODIFY COLUMN name VARCHAR(100) NOT NULL;

ALTER TABLE properties
    MODIFY COLUMN deposit_amount BIGINT NULL,
    MODIFY COLUMN monthly_rent_amount BIGINT NULL,
    MODIFY COLUMN discovery_source VARCHAR(500) NULL,
    MODIFY COLUMN last_activity_at DATETIME(6) NULL,
    MODIFY COLUMN created_at DATETIME(6) NULL,
    MODIFY COLUMN updated_at DATETIME(6) NULL;

ALTER TABLE property_photos
    DROP CHECK ck_property_photos_content_type,
    DROP CHECK ck_property_photos_size;

CREATE TABLE main_property_photos
(
    id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    property_id        BIGINT NOT NULL,
    property_photos_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_main_property_photos_pair UNIQUE (property_id, property_photos_id),
    CONSTRAINT fk_main_property_photos_property
        FOREIGN KEY (property_id) REFERENCES properties (id) ON DELETE CASCADE,
    CONSTRAINT fk_main_property_photos_photo
        FOREIGN KEY (property_photos_id) REFERENCES property_photos (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE system_memo_items
(
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    label         VARCHAR(30)    NOT NULL,
    display_order SMALLINT UNSIGNED NOT NULL,
    deleted_at    DATETIME(6)    NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE property_memos
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    property_id BIGINT NOT NULL,
    free_memo   VARCHAR(2000)   NOT NULL DEFAULT '',
    PRIMARY KEY (id),
    CONSTRAINT uk_property_memos_property UNIQUE (property_id),
    CONSTRAINT fk_property_memos_property
        FOREIGN KEY (property_id) REFERENCES properties (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE property_memo_items
(
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    property_memo_id BIGINT UNSIGNED NOT NULL,
    system_meno_id   BIGINT UNSIGNED NOT NULL,
    content          VARCHAR(200)   NOT NULL DEFAULT '',
    PRIMARY KEY (id),
    CONSTRAINT fk_property_memo_items_memo
        FOREIGN KEY (property_memo_id) REFERENCES property_memos (id) ON DELETE CASCADE,
    CONSTRAINT fk_property_memo_items_system
        FOREIGN KEY (system_meno_id) REFERENCES system_memo_items (id),
    CONSTRAINT uk_property_memo_items_system UNIQUE (property_memo_id, system_meno_id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE system_check_items
(
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    stage      VARCHAR(30)     NOT NULL,
    item_type  VARCHAR(20)     NOT NULL,
    question   VARCHAR(200)    NOT NULL,
    deleted_at DATETIME(6)     NULL,
    PRIMARY KEY (id),
    INDEX idx_system_check_items_stage_active (stage, deleted_at, item_type, id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE user_checklists
(
    id        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    name      VARCHAR(50)    NOT NULL,
    stage     VARCHAR(30)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_checklists_member
        FOREIGN KEY (member_id) REFERENCES members (id),
    INDEX idx_user_checklists_member_stage (member_id, stage, id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE user_checklist_items
(
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_checklist_id    BIGINT UNSIGNED NOT NULL,
    system_check_item_id BIGINT UNSIGNED NOT NULL,
    display_order        SMALLINT UNSIGNED NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_checklist_items_checklist
        FOREIGN KEY (user_checklist_id) REFERENCES user_checklists (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_checklist_items_system_item
        FOREIGN KEY (system_check_item_id) REFERENCES system_check_items (id),
    CONSTRAINT uk_user_checklist_items_system UNIQUE (user_checklist_id, system_check_item_id),
    CONSTRAINT uk_user_checklist_items_order UNIQUE (user_checklist_id, display_order)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE property_checklists
(
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    property_id       BIGINT NOT NULL,
    user_checklist_id BIGINT UNSIGNED NULL,
    checklist_name    VARCHAR(50)    NOT NULL,
    stage             VARCHAR(30)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_property_checklists_property
        FOREIGN KEY (property_id) REFERENCES properties (id) ON DELETE CASCADE,
    CONSTRAINT fk_property_checklists_source
        FOREIGN KEY (user_checklist_id) REFERENCES user_checklists (id) ON DELETE SET NULL,
    CONSTRAINT uk_property_checklists_property_stage UNIQUE (property_id, stage)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE property_checklist_items
(
    id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    property_checklist_id BIGINT UNSIGNED NOT NULL,
    system_check_item_id  BIGINT UNSIGNED NOT NULL,
    display_order         SMALLINT UNSIGNED NOT NULL,
    status                VARCHAR(20) NOT NULL DEFAULT 'UNCONFIRMED',
    memo                  VARCHAR(500) NOT NULL DEFAULT '',
    question              VARCHAR(200) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_property_checklist_items_checklist
        FOREIGN KEY (property_checklist_id) REFERENCES property_checklists (id) ON DELETE CASCADE,
    CONSTRAINT fk_property_checklist_items_system_item
        FOREIGN KEY (system_check_item_id) REFERENCES system_check_items (id),
    CONSTRAINT uk_property_checklist_items_system UNIQUE (property_checklist_id, system_check_item_id),
    CONSTRAINT uk_property_checklist_items_order UNIQUE (property_checklist_id, display_order)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Access Token만 사용하므로 Refresh Token 저장 테이블을 제거한다.
DROP TABLE IF EXISTS refresh_tokens;

-- 회원 식별자는 OAuth subject가 아니라 members.id를 사용한다.
-- 기존 레거시 OAuth 컬럼은 데이터 손실 없이 nullable로 전환한다.
ALTER TABLE members
    MODIFY COLUMN oauth_provider VARCHAR(20) NULL,
    MODIFY COLUMN oauth_subject VARCHAR(255) NULL,
    MODIFY COLUMN display_name VARCHAR(100) NULL,
    DROP COLUMN last_login_at;
