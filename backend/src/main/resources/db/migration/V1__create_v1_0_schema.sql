-- v1.0의 스키마와 안정 ID 기준 데이터를 그대로 재현한다.
CREATE TABLE IF NOT EXISTS members
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    oauth_provider VARCHAR(20)  NOT NULL,
    oauth_subject  VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    email           VARCHAR(320) NOT NULL,
    display_name    VARCHAR(100) NOT NULL,
    last_login_at   DATETIME(6)  NOT NULL,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_members_provider_subject UNIQUE (oauth_provider, oauth_subject),
    CONSTRAINT ck_members_oauth_provider CHECK (oauth_provider = 'GOOGLE')
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS properties
(
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id             BIGINT       NOT NULL,
    name                  VARCHAR(50)  NOT NULL,
    deposit_amount        BIGINT       NOT NULL,
    monthly_rent_amount   BIGINT       NOT NULL,
    discovery_source_type VARCHAR(20)  NOT NULL,
    discovery_source      VARCHAR(500) NOT NULL,
    memo                  VARCHAR(5000) NOT NULL DEFAULT '',
    memo_updated_at       DATETIME(6)  NULL,
    last_activity_at      DATETIME(6)  NOT NULL,
    created_at            DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at            DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_properties_member FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT uk_properties_id_member UNIQUE (id, member_id),
    CONSTRAINT ck_properties_source_type CHECK (discovery_source_type IN ('URL', 'TEXT')),
    CONSTRAINT ck_properties_deposit_amount CHECK (deposit_amount BETWEEN 0 AND 9007199254740991),
    CONSTRAINT ck_properties_monthly_rent_amount CHECK (monthly_rent_amount BETWEEN 0 AND 9007199254740991),
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
    checksum_sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    created_at      DATETIME(6)  NOT NULL,
    CONSTRAINT fk_property_photos_property_owner
        FOREIGN KEY (property_id, member_id) REFERENCES properties (id, member_id) ON DELETE CASCADE,
    CONSTRAINT uk_property_photos_storage_key UNIQUE (storage_key),
    CONSTRAINT ck_property_photos_content_type
        CHECK (content_type IN ('image/jpeg', 'image/png', 'image/webp')),
    CONSTRAINT ck_property_photos_size CHECK (size_bytes > 0 AND size_bytes <= 10485760),
    INDEX idx_property_photos_property_created (property_id, created_at, id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS check_items
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    stage      VARCHAR(30)   NOT NULL,
    question   VARCHAR(500)  NOT NULL,
    guide      VARCHAR(1000) NULL,
    is_active  BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_check_items_id_stage UNIQUE (id, stage),
    CONSTRAINT uk_check_items_stage_question UNIQUE (stage, question),
    CONSTRAINT ck_check_items_stage CHECK (stage IN ('ONLINE_PHONE', 'ON_SITE', 'PRE_CONTRACT')),
    INDEX idx_check_items_stage_active (stage, is_active, id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS checklist_presets
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    preset_type VARCHAR(20)  NOT NULL,
    stage       VARCHAR(30)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_checklist_presets_type_stage UNIQUE (preset_type, stage),
    CONSTRAINT uk_checklist_presets_id_stage UNIQUE (id, stage),
    CONSTRAINT ck_checklist_presets_type CHECK (preset_type IN ('ONE_ROOM', 'GOSHIWON')),
    CONSTRAINT ck_checklist_presets_stage CHECK (stage IN ('ONLINE_PHONE', 'ON_SITE', 'PRE_CONTRACT'))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS checklist_preset_items
(
    preset_id    BIGINT            NOT NULL,
    check_item_id BIGINT           NOT NULL,
    stage        VARCHAR(30)       NOT NULL,
    item_order   SMALLINT UNSIGNED NOT NULL,
    PRIMARY KEY (preset_id, check_item_id),
    CONSTRAINT uk_preset_items_order UNIQUE (preset_id, item_order),
    CONSTRAINT fk_preset_items_preset_stage
        FOREIGN KEY (preset_id, stage) REFERENCES checklist_presets (id, stage) ON DELETE CASCADE,
    CONSTRAINT fk_preset_items_check_item_stage
        FOREIGN KEY (check_item_id, stage) REFERENCES check_items (id, stage) ON DELETE RESTRICT,
    CONSTRAINT ck_preset_items_order CHECK (item_order >= 1)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS checklists
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id  BIGINT      NOT NULL,
    name       VARCHAR(50) NOT NULL,
    stage      VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_checklists_member FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT uk_checklists_id_member_stage UNIQUE (id, member_id, stage),
    CONSTRAINT uk_checklists_id_stage UNIQUE (id, stage),
    CONSTRAINT ck_checklists_name CHECK (CHAR_LENGTH(TRIM(name)) BETWEEN 1 AND 50),
    CONSTRAINT ck_checklists_stage CHECK (stage IN ('ONLINE_PHONE', 'ON_SITE', 'PRE_CONTRACT')),
    INDEX idx_checklists_member_stage_updated (member_id, stage, updated_at DESC, id DESC)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS checklist_items
(
    checklist_id BIGINT            NOT NULL,
    check_item_id BIGINT           NOT NULL,
    stage         VARCHAR(30)       NOT NULL,
    item_order    SMALLINT UNSIGNED NOT NULL,
    PRIMARY KEY (checklist_id, check_item_id),
    CONSTRAINT uk_checklist_items_order UNIQUE (checklist_id, item_order),
    CONSTRAINT fk_checklist_items_checklist_stage
        FOREIGN KEY (checklist_id, stage) REFERENCES checklists (id, stage) ON DELETE CASCADE,
    CONSTRAINT fk_checklist_items_check_item_stage
        FOREIGN KEY (check_item_id, stage) REFERENCES check_items (id, stage) ON DELETE RESTRICT,
    CONSTRAINT ck_checklist_items_order CHECK (item_order >= 1)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS property_active_checklists
(
    property_id  BIGINT      NOT NULL,
    member_id    BIGINT      NOT NULL,
    stage        VARCHAR(30) NOT NULL,
    checklist_id BIGINT      NOT NULL,
    created_at   DATETIME(6) NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (property_id, stage),
    CONSTRAINT fk_active_checklists_property_owner
        FOREIGN KEY (property_id, member_id) REFERENCES properties (id, member_id) ON DELETE CASCADE,
    CONSTRAINT fk_active_checklists_checklist_owner_stage
        FOREIGN KEY (checklist_id, member_id, stage)
            REFERENCES checklists (id, member_id, stage) ON DELETE CASCADE,
    CONSTRAINT ck_active_checklists_stage
        CHECK (stage IN ('ONLINE_PHONE', 'ON_SITE', 'PRE_CONTRACT')),
    INDEX idx_active_checklists_checklist (checklist_id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS visits
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id  BIGINT      NOT NULL,
    member_id    BIGINT      NOT NULL,
    status       VARCHAR(20) NOT NULL,
    started_at   DATETIME(6) NOT NULL,
    completed_at DATETIME(6) NULL,
    updated_at   DATETIME(6) NOT NULL,
    CONSTRAINT fk_visits_property_owner
        FOREIGN KEY (property_id, member_id) REFERENCES properties (id, member_id) ON DELETE CASCADE,
    CONSTRAINT ck_visits_status CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
    CONSTRAINT ck_visits_completion CHECK (
        (status = 'IN_PROGRESS' AND completed_at IS NULL)
        OR (status = 'COMPLETED' AND completed_at IS NOT NULL)
    ),
    INDEX idx_visits_property_started (property_id, started_at DESC, id DESC),
    INDEX idx_visits_member_started (member_id, started_at DESC, id DESC)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS visit_stage_snapshots
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    visit_id            BIGINT      NOT NULL,
    stage               VARCHAR(30) NOT NULL,
    source_checklist_id BIGINT      NULL,
    checklist_name      VARCHAR(50) NOT NULL,
    created_at          DATETIME(6) NOT NULL,
    CONSTRAINT uk_visit_snapshots_visit_stage UNIQUE (visit_id, stage),
    CONSTRAINT uk_visit_snapshots_id_stage UNIQUE (id, stage),
    CONSTRAINT fk_visit_snapshots_visit
        FOREIGN KEY (visit_id) REFERENCES visits (id) ON DELETE CASCADE,
    CONSTRAINT fk_visit_snapshots_source_checklist
        FOREIGN KEY (source_checklist_id) REFERENCES checklists (id) ON DELETE SET NULL,
    CONSTRAINT ck_visit_snapshots_stage
        CHECK (stage IN ('ONLINE_PHONE', 'ON_SITE', 'PRE_CONTRACT'))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS visit_check_items
(
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    visit_stage_snapshot_id BIGINT            NOT NULL,
    stage                   VARCHAR(30)       NOT NULL,
    source_check_item_id    BIGINT            NOT NULL,
    question_snapshot       VARCHAR(500)      NOT NULL,
    guide_snapshot          VARCHAR(1000)     NULL,
    item_order              SMALLINT UNSIGNED NOT NULL,
    status                  VARCHAR(20)       NOT NULL DEFAULT 'UNCONFIRMED',
    version                 BIGINT            NOT NULL DEFAULT 0,
    created_at              DATETIME(6)       NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at              DATETIME(6)       NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_visit_items_source UNIQUE (visit_stage_snapshot_id, source_check_item_id),
    CONSTRAINT uk_visit_items_order UNIQUE (visit_stage_snapshot_id, item_order),
    CONSTRAINT fk_visit_items_snapshot_stage
        FOREIGN KEY (visit_stage_snapshot_id, stage)
            REFERENCES visit_stage_snapshots (id, stage) ON DELETE CASCADE,
    CONSTRAINT fk_visit_items_source_stage
        FOREIGN KEY (source_check_item_id, stage)
            REFERENCES check_items (id, stage) ON DELETE RESTRICT,
    CONSTRAINT ck_visit_items_stage
        CHECK (stage IN ('ONLINE_PHONE', 'ON_SITE', 'PRE_CONTRACT')),
    CONSTRAINT ck_visit_items_status
        CHECK (status IN ('GOOD', 'CAUTION', 'UNCONFIRMED')),
    CONSTRAINT ck_visit_items_order CHECK (item_order >= 1),
    CONSTRAINT ck_visit_items_version CHECK (version >= 0)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

INSERT IGNORE INTO check_items (id, stage, question, guide, is_active) VALUES
    (101, 'ON_SITE', '보일러가 정상 작동하는가?', '온수와 난방을 직접 작동해 확인합니다.', TRUE),
    (102, 'ON_SITE', '채광과 창문 방향이 괜찮은가?', NULL, TRUE),
    (103, 'ON_SITE', '환기가 잘 되는가?', NULL, TRUE),
    (104, 'ON_SITE', '사생활 보호가 가능한가?', NULL, TRUE),
    (105, 'ON_SITE', '곰팡이 흔적이 없는가?', NULL, TRUE),
    (106, 'ON_SITE', '결로 흔적이 없는가?', NULL, TRUE),
    (107, 'ON_SITE', '벽이나 천장에 누수 흔적이 없는가?', NULL, TRUE),
    (108, 'ON_SITE', '싱크대·세면대 아래 누수가 없는가?', NULL, TRUE),
    (109, 'ON_SITE', '악취나 벌레 흔적이 없는가?', NULL, TRUE),
    (110, 'ON_SITE', '수도 수압이 충분한가?', NULL, TRUE),
    (111, 'ON_SITE', '온수가 잘 나오는가?', NULL, TRUE),
    (112, 'ON_SITE', '화장실 배수가 잘 되는가?', NULL, TRUE),
    (113, 'ON_SITE', '화장실 환기가 잘 되는가?', NULL, TRUE),
    (114, 'ON_SITE', '배수구에서 냄새가 나지 않는가?', NULL, TRUE),
    (115, 'ON_SITE', '외부 소음이 심하지 않은가?', NULL, TRUE),
    (116, 'ON_SITE', '층간·벽간소음이 심하지 않은가?', NULL, TRUE),
    (117, 'ON_SITE', '휴대전화가 잘 터지는가?', NULL, TRUE),
    (118, 'ON_SITE', '콘센트 위치와 개수가 충분한가?', NULL, TRUE),
    (119, 'ON_SITE', '난방 방식과 비용을 확인했는가?', NULL, TRUE),
    (120, 'ON_SITE', '전기·수도·가스가 개별 계량되는가?', NULL, TRUE),
    (121, 'ON_SITE', '필요한 가구를 배치할 공간이 충분한가?', NULL, TRUE),
    (122, 'ON_SITE', '생활 동선이 불편하지 않은가?', NULL, TRUE),
    (123, 'ON_SITE', '수납공간이 충분한가?', NULL, TRUE),
    (124, 'ON_SITE', '문 폭이 이삿짐을 옮기기에 충분한가?', NULL, TRUE),
    (125, 'ON_SITE', '에어컨이 정상 작동하는가?', NULL, TRUE),
    (126, 'ON_SITE', '냉장고가 정상 작동하는가?', NULL, TRUE),
    (127, 'ON_SITE', '세탁기가 정상 작동하는가?', NULL, TRUE),
    (128, 'ON_SITE', '파손·오염·옵션 상태를 사진으로 남겼는가?', NULL, TRUE),
    (129, 'ON_SITE', '공동현관 잠금장치가 있는가?', NULL, TRUE),
    (130, 'ON_SITE', 'CCTV가 있는가?', NULL, TRUE),
    (131, 'ON_SITE', '현관문·창문 잠금장치가 정상 작동하는가?', NULL, TRUE),
    (132, 'ON_SITE', '복도·계단·엘리베이터 관리 상태가 괜찮은가?', NULL, TRUE),
    (133, 'ON_SITE', '소화기·화재감지기·비상구가 있는가?', NULL, TRUE),
    (134, 'ON_SITE', '역·정류장에서 집까지 직접 걸어봤는가?', NULL, TRUE),
    (135, 'ON_SITE', '언덕이나 이동이 불편한 구간이 있는가?', NULL, TRUE),
    (136, 'ON_SITE', '야간 귀가 동선이 위험하지 않은가?', NULL, TRUE),
    (137, 'ON_SITE', '쓰레기·재활용·음식물 배출이 편한가?', NULL, TRUE),
    (138, 'ON_SITE', '편의점·마트·병원·약국 등 생활시설이 가까운가?', NULL, TRUE),
    (201, 'ONLINE_PHONE', '보증금과 월세, 관리비를 확인했는가?', NULL, TRUE),
    (202, 'ONLINE_PHONE', '관리비에 어떤 항목이 포함되는가?', NULL, TRUE),
    (203, 'ONLINE_PHONE', '입주 가능일은 언제인가?', NULL, TRUE),
    (204, 'ONLINE_PHONE', '주변 시세보다 지나치게 저렴하지 않은가?', NULL, TRUE),
    (205, 'ONLINE_PHONE', '위치와 통학·통근 시간이 괜찮은가?', NULL, TRUE),
    (206, 'ONLINE_PHONE', '매물 사진과 정보가 충분한가?', NULL, TRUE),
    (207, 'ONLINE_PHONE', '현재 실제로 계약 가능한 매물인가?', NULL, TRUE),
    (208, 'ONLINE_PHONE', '광고에 나온 보증금·월세 조건이 맞는가?', NULL, TRUE),
    (209, 'ONLINE_PHONE', '관리비와 별도 공과금은 어떻게 되는가?', NULL, TRUE),
    (210, 'ONLINE_PHONE', '옵션에는 무엇이 포함되어 있는가?', NULL, TRUE),
    (211, 'ONLINE_PHONE', '입주 가능일을 중개사에게 확인했는가?', NULL, TRUE),
    (212, 'ONLINE_PHONE', '전입신고가 가능한가?', NULL, TRUE),
    (213, 'ONLINE_PHONE', '대출이 필요한 경우 가능한 매물인가?', NULL, TRUE),
    (214, 'ONLINE_PHONE', '보증보험 가입이 가능한가?', NULL, TRUE),
    (215, 'ONLINE_PHONE', '방문 전 가계약금이나 예약금을 요구하는가?', NULL, TRUE),
    (301, 'PRE_CONTRACT', '보증금·월세·관리비를 포함한 총주거비를 확인했는가?', NULL, TRUE),
    (302, 'PRE_CONTRACT', '건축물대장 주소·용도·위반 여부를 확인했는가?', NULL, TRUE),
    (303, 'PRE_CONTRACT', '등기사항증명서의 소유자를 확인했는가?', NULL, TRUE),
    (304, 'PRE_CONTRACT', '근저당·압류 등 권리관계를 확인했는가?', NULL, TRUE),
    (305, 'PRE_CONTRACT', '보증금이 주변 시세와 비교해 적절한가?', NULL, TRUE),
    (306, 'PRE_CONTRACT', '대출 가능 여부를 확인했는가?', NULL, TRUE),
    (307, 'PRE_CONTRACT', '보증보험 가입 가능 여부를 확인했는가?', NULL, TRUE),
    (308, 'PRE_CONTRACT', '필요한 수리 내용과 비용 부담자를 확인했는가?', NULL, TRUE),
    (309, 'PRE_CONTRACT', '중요한 약속을 문자 등 기록으로 남겼는가?', NULL, TRUE),
    (310, 'PRE_CONTRACT', '계약 당일 최신 등기사항증명서를 확인했는가?', NULL, TRUE),
    (311, 'PRE_CONTRACT', '계약 상대방과 등기상 소유자가 같은가?', NULL, TRUE),
    (312, 'PRE_CONTRACT', '송금 계좌 명의가 소유자인가?', NULL, TRUE),
    (313, 'PRE_CONTRACT', '보증금·월세·납부일·계약기간이 명확한가?', NULL, TRUE),
    (314, 'PRE_CONTRACT', '관리비 포함·별도 항목이 명확한가?', NULL, TRUE),
    (315, 'PRE_CONTRACT', '옵션·파손·수리 약속이 계약서 또는 특약에 있는가?', NULL, TRUE),
    (316, 'PRE_CONTRACT', '대출·보증보험 불가 시 계약금 반환 조건이 있는가?', NULL, TRUE),
    (317, 'PRE_CONTRACT', '잔금 전 신규 권리 설정 금지 특약을 확인했는가?', NULL, TRUE),
    (318, 'PRE_CONTRACT', '계약서에 빈칸이 없는가?', NULL, TRUE),
    (319, 'PRE_CONTRACT', '계약서·확인설명서·영수증 등을 받았는가?', NULL, TRUE);

INSERT IGNORE INTO checklist_presets (id, preset_type, stage, name, is_active) VALUES
    (1, 'ONE_ROOM', 'ONLINE_PHONE', '원룸 온라인·전화 체크리스트', TRUE),
    (2, 'ONE_ROOM', 'ON_SITE', '원룸 현장 체크리스트', TRUE),
    (3, 'ONE_ROOM', 'PRE_CONTRACT', '원룸 계약 전 체크리스트', TRUE),
    (4, 'GOSHIWON', 'ONLINE_PHONE', '고시원 온라인·전화 체크리스트', TRUE),
    (5, 'GOSHIWON', 'ON_SITE', '고시원 현장 체크리스트', TRUE),
    (6, 'GOSHIWON', 'PRE_CONTRACT', '고시원 계약 전 체크리스트', TRUE);

INSERT IGNORE INTO checklist_preset_items (preset_id, check_item_id, stage, item_order)
SELECT preset.id,
       item.id,
       preset.stage,
       ROW_NUMBER() OVER (PARTITION BY preset.id ORDER BY item.id)
FROM checklist_presets preset
JOIN check_items item ON item.stage = preset.stage
WHERE preset.is_active = TRUE
  AND item.is_active = TRUE;
