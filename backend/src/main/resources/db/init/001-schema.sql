-- MVP2의 현재 애플리케이션이 사용하는 스키마 정본이다.
-- 과거 누적 마이그레이션은 mvp1-baseline 태그에서 확인한다.

CREATE TABLE IF NOT EXISTS members
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(320) NOT NULL,
    name          VARCHAR(100) NOT NULL,
    last_login_at DATETIME(6)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    CONSTRAINT uk_members_email UNIQUE (email)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS nickname_credentials
(
    member_id     BIGINT       NOT NULL,
    nickname      VARCHAR(100) NOT NULL,
    nickname_key  VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    password_hash VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (member_id),
    CONSTRAINT uk_nickname_credentials_key UNIQUE (nickname_key),
    CONSTRAINT fk_nickname_credentials_member
        FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS properties
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id           BIGINT       NOT NULL,
    name                VARCHAR(50)  NOT NULL,
    deposit_amount      BIGINT       NOT NULL DEFAULT 0,
    monthly_rent_amount BIGINT       NOT NULL DEFAULT 0,
    discovery_source    VARCHAR(500) NOT NULL DEFAULT '',
    road_address        VARCHAR(255) NULL,
    jibun_address       VARCHAR(255) NULL,
    latitude            DECIMAL(10, 7) NULL,
    longitude           DECIMAL(11, 7) NULL,
    created_at          DATETIME(6) NOT NULL,
    updated_at          DATETIME(6) NOT NULL,
    last_activity_at    DATETIME(6) NOT NULL,
    CONSTRAINT fk_properties_member
        FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE,
    CONSTRAINT uk_properties_id_member UNIQUE (id, member_id),
    CONSTRAINT ck_properties_location_pair
        CHECK ((latitude IS NULL AND longitude IS NULL) OR (latitude IS NOT NULL AND longitude IS NOT NULL)),
    CONSTRAINT ck_properties_latitude CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_properties_longitude CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180),
    INDEX idx_properties_member_activity (member_id, last_activity_at DESC, id DESC)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS property_photos
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id     BIGINT       NOT NULL,
    member_id       BIGINT       NOT NULL,
    storage_key     VARCHAR(512) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    content_type    VARCHAR(100) NOT NULL,
    size_bytes      BIGINT       NOT NULL,
    checksum_sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at      DATETIME(6)  NOT NULL,
    CONSTRAINT fk_property_photos_property_owner
        FOREIGN KEY (property_id, member_id) REFERENCES properties (id, member_id) ON DELETE CASCADE,
    CONSTRAINT uk_property_photos_storage_key UNIQUE (storage_key),
    CONSTRAINT uk_property_photos_property_id UNIQUE (property_id, id),
    INDEX idx_property_photos_property_created (property_id, created_at, id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS main_property_photos
(
    id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    property_id        BIGINT NOT NULL,
    property_photos_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_main_property_photos_property UNIQUE (property_id),
    CONSTRAINT uk_main_property_photos_pair UNIQUE (property_id, property_photos_id),
    CONSTRAINT fk_main_property_photos_property
        FOREIGN KEY (property_id) REFERENCES properties (id) ON DELETE CASCADE,
    CONSTRAINT fk_main_property_photos_photo_owner
        FOREIGN KEY (property_id, property_photos_id)
            REFERENCES property_photos (property_id, id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS system_memo_items
(
    id            BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT,
    label         VARCHAR(30)       NOT NULL,
    display_order SMALLINT UNSIGNED NOT NULL,
    deleted_at    DATETIME(6)       NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS property_memos
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    property_id BIGINT          NOT NULL,
    free_memo   VARCHAR(2000)   NOT NULL DEFAULT '',
    PRIMARY KEY (id),
    CONSTRAINT uk_property_memos_property UNIQUE (property_id),
    CONSTRAINT fk_property_memos_property
        FOREIGN KEY (property_id) REFERENCES properties (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS property_memo_items
(
    id                  BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT,
    property_memo_id    BIGINT UNSIGNED   NOT NULL,
    system_memo_item_id BIGINT UNSIGNED   NOT NULL,
    label               VARCHAR(30)       NOT NULL,
    display_order       SMALLINT UNSIGNED NOT NULL,
    content             VARCHAR(200)      NOT NULL DEFAULT '',
    PRIMARY KEY (id),
    CONSTRAINT fk_property_memo_items_memo
        FOREIGN KEY (property_memo_id) REFERENCES property_memos (id) ON DELETE CASCADE,
    CONSTRAINT fk_property_memo_items_system
        FOREIGN KEY (system_memo_item_id) REFERENCES system_memo_items (id),
    CONSTRAINT uk_property_memo_items_system UNIQUE (property_memo_id, system_memo_item_id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS system_check_items
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

CREATE TABLE IF NOT EXISTS user_checklists
(
    id        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    member_id BIGINT          NOT NULL,
    name      VARCHAR(50)     NOT NULL,
    stage     VARCHAR(30)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_checklists_member
        FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE,
    INDEX idx_user_checklists_member_stage (member_id, stage, id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS user_checklist_items
(
    id                   BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT,
    user_checklist_id    BIGINT UNSIGNED   NOT NULL,
    system_check_item_id BIGINT UNSIGNED   NULL,
    stage                VARCHAR(30)       NOT NULL,
    item_type            VARCHAR(20)       NOT NULL,
    question             VARCHAR(200)      NOT NULL,
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

CREATE TABLE IF NOT EXISTS property_checklists
(
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    property_id       BIGINT          NOT NULL,
    user_checklist_id BIGINT UNSIGNED NULL,
    checklist_name    VARCHAR(50)     NOT NULL,
    stage             VARCHAR(30)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_property_checklists_property
        FOREIGN KEY (property_id) REFERENCES properties (id) ON DELETE CASCADE,
    CONSTRAINT fk_property_checklists_source
        FOREIGN KEY (user_checklist_id) REFERENCES user_checklists (id) ON DELETE SET NULL,
    CONSTRAINT uk_property_checklists_property_stage UNIQUE (property_id, stage)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS property_checklist_items
(
    id                    BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT,
    property_checklist_id BIGINT UNSIGNED   NOT NULL,
    system_check_item_id  BIGINT UNSIGNED   NULL,
    display_order         SMALLINT UNSIGNED NOT NULL,
    status                VARCHAR(20)       NOT NULL DEFAULT 'UNCONFIRMED',
    memo                  VARCHAR(500)      NOT NULL DEFAULT '',
    question              VARCHAR(200)      NOT NULL,
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
