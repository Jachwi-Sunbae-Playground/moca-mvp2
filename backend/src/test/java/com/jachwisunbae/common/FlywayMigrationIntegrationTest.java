package com.jachwisunbae.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class FlywayMigrationIntegrationTest extends IntegrationTest {

    private String databaseName;
    private DriverManagerDataSource dataSource;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void createIsolatedDatabase() throws SQLException {
        databaseName = "migration_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = rootConnection()) {
            connection.createStatement().execute(
                    "CREATE DATABASE " + databaseName
                            + " CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci"
            );
        }
        dataSource = new DriverManagerDataSource(databaseUrl(), "root", MYSQL_CONTAINER.getPassword());
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @AfterEach
    void dropIsolatedDatabase() throws SQLException {
        try (Connection connection = rootConnection()) {
            connection.createStatement().execute("DROP DATABASE IF EXISTS " + databaseName);
        }
    }

    @DisplayName("빈 DB는 전체 migration을 한 번만 적용하고 checksum 변경을 검증 실패로 처리한다")
    @Test
    void installEmptyDatabaseOnlyOnceAndValidateChecksums() {
        final Flyway flyway = flyway();

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(4);
        flyway.validate();
        assertThat(successfulHistoryCount()).isEqualTo(4L);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM check_items", Long.class)).isEqualTo(72L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM checklist_presets WHERE is_active = TRUE",
                Long.class
        )).isEqualTo(3L);

        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(successfulHistoryCount()).isEqualTo(4L);

        jdbcTemplate.update(
                "UPDATE flyway_schema_history SET checksum = checksum + 1 WHERE version = '2'"
        );
        assertThatThrownBy(flyway::validate).isInstanceOf(FlywayException.class);
    }

    @DisplayName("pre-Flyway v1.0 DB는 명시적 baseline 뒤 데이터 손실 없이 v1.1로 이전한다")
    @Test
    void baselineAndMigrateLegacyDatabaseWithoutDataLoss() {
        installLegacySchemaAndData();
        final Flyway flyway = flyway();

        assertThatThrownBy(flyway::migrate)
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("non-empty schema");

        assertThat(baselineFlyway().migrate().migrationsExecuted).isZero();
        assertThat(successfulHistoryCount()).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE type = 'BASELINE' AND version = '1'",
                Long.class
        )).isEqualTo(1L);
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(3);
        flyway.validate();

        assertPreservedLegacyRows();
        assertBackfilledV11Values();
        assertExpandedConstraintsRejectInvalidRows();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    private Flyway flyway() {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineVersion("1")
                .baselineDescription("v1.0 pre-Flyway schema")
                .baselineOnMigrate(false)
                .cleanDisabled(true)
                .validateOnMigrate(true)
                .load();
    }

    private Flyway baselineFlyway() {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineVersion("1")
                .baselineDescription("v1.0 pre-Flyway schema")
                .baselineOnMigrate(true)
                .target("1")
                .cleanDisabled(true)
                .validateOnMigrate(true)
                .load();
    }

    private void installLegacySchemaAndData() {
        final ResourceDatabasePopulator schema = new ResourceDatabasePopulator(
                new ClassPathResource("db/migration/V1__create_v1_0_schema.sql")
        );
        DatabasePopulatorUtils.execute(schema, dataSource);

        jdbcTemplate.update("""
                INSERT INTO members (
                    id, oauth_provider, oauth_subject, email, display_name,
                    last_login_at, created_at, updated_at
                ) VALUES (9001, 'GOOGLE', 'legacy-member', 'legacy@example.com', '기존 회원',
                    '2026-08-10 00:00:00.000000', '2026-08-10 00:00:00.000000',
                    '2026-08-10 00:00:00.000000')
                """);
        jdbcTemplate.update("""
                INSERT INTO properties (
                    id, member_id, name, deposit_amount, monthly_rent_amount,
                    discovery_source_type, discovery_source, memo, memo_updated_at,
                    last_activity_at, created_at, updated_at
                ) VALUES (9101, 9001, '기존 고시원 기록', 1000000, 450000,
                    'TEXT', '중개사 소개', '계약 전 확인할 자유 메모',
                    '2026-08-10 01:02:03.123456', '2026-08-10 03:00:00.000000',
                    '2026-08-10 00:30:00.000000', '2026-08-10 03:00:00.000000')
                """);
        jdbcTemplate.update("""
                INSERT INTO properties (
                    id, member_id, name, deposit_amount, monthly_rent_amount,
                    discovery_source_type, discovery_source, memo, memo_updated_at,
                    last_activity_at, created_at, updated_at
                ) VALUES (9102, 9001, '메모를 저장하지 않은 기존 매물', 0, 0,
                    'TEXT', '직접 발견', '', NULL,
                    '2026-08-10 04:00:00.000000', '2026-08-10 04:00:00.000000',
                    '2026-08-10 04:30:00.333333')
                """);
        jdbcTemplate.update("""
                INSERT INTO property_photos (
                    id, property_id, member_id, storage_key, content_type, size_bytes, created_at
                ) VALUES (9201, 9101, 9001, 'members/9001/properties/9101/legacy-photo',
                    'image/jpeg', 1024, '2026-08-10 01:10:00.000000')
                """);
        jdbcTemplate.update("""
                INSERT INTO checklists (id, member_id, name, stage, created_at, updated_at)
                VALUES (9301, 9001, '기존 고시원 현장 체크리스트', 'ON_SITE',
                    '2026-08-10 01:20:00.000000', '2026-08-10 01:20:00.000000')
                """);
        jdbcTemplate.update("""
                INSERT INTO checklist_items (checklist_id, check_item_id, stage, item_order)
                VALUES (9301, 101, 'ON_SITE', 1), (9301, 110, 'ON_SITE', 2)
                """);
        jdbcTemplate.update("""
                INSERT INTO property_active_checklists (
                    property_id, member_id, stage, checklist_id, created_at, updated_at
                ) VALUES (9101, 9001, 'ON_SITE', 9301,
                    '2026-08-10 01:30:00.000000', '2026-08-10 01:30:00.000000')
                """);
        jdbcTemplate.update("""
                INSERT INTO visits (
                    id, property_id, member_id, status, started_at, completed_at, updated_at
                ) VALUES (9401, 9101, 9001, 'COMPLETED',
                    '2026-08-10 02:00:00.000000', '2026-08-10 02:30:00.000000',
                    '2026-08-10 02:40:00.000000')
                """);
        jdbcTemplate.update("""
                INSERT INTO visit_stage_snapshots (
                    id, visit_id, stage, source_checklist_id, checklist_name, created_at
                ) VALUES (9501, 9401, 'ON_SITE', 9301, '기존 고시원 현장 체크리스트',
                    '2026-08-10 02:00:00.000000')
                """);
        jdbcTemplate.update("""
                INSERT INTO visit_check_items (
                    id, visit_stage_snapshot_id, stage, source_check_item_id,
                    question_snapshot, guide_snapshot, item_order, status, version,
                    created_at, updated_at
                ) VALUES
                    (9601, 9501, 'ON_SITE', 101, '과거 보일러 질문', '과거 보일러 안내',
                        1, 'GOOD', 4, '2026-08-10 02:00:00.000000', '2026-08-10 02:10:00.111111'),
                    (9602, 9501, 'ON_SITE', 110, '과거 수압 질문', NULL,
                        2, 'CAUTION', 7, '2026-08-10 02:00:00.000000', '2026-08-10 02:20:00.222222')
                """);
    }

    private void assertPreservedLegacyRows() {
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM members", Long.class)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM properties", Long.class)).isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM property_photos", Long.class)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM checklists", Long.class)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM checklist_items", Long.class)).isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM property_active_checklists",
                Long.class
        )).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM visits", Long.class)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM visit_stage_snapshots",
                Long.class
        )).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM visit_check_items", Long.class)).isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM checklist_preset_items",
                Long.class
        )).isEqualTo(144L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT member_id FROM properties WHERE id = 9101",
                Long.class
        )).isEqualTo(9001L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT property_id FROM property_photos WHERE id = 9201",
                Long.class
        )).isEqualTo(9101L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT checklist_id FROM property_active_checklists WHERE property_id = 9101",
                Long.class
        )).isEqualTo(9301L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT source_checklist_id FROM visit_stage_snapshots WHERE id = 9501",
                Long.class
        )).isEqualTo(9301L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT question_snapshot FROM visit_check_items WHERE id = 9601",
                String.class
        )).isEqualTo("과거 보일러 질문");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT version FROM visit_check_items WHERE id = 9602",
                Long.class
        )).isEqualTo(7L);
    }

    private void assertBackfilledV11Values() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT additional_memo FROM property_pre_visit_memos WHERE property_id = 9101",
                String.class
        )).isEqualTo("계약 전 확인할 자유 메모");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM property_pre_visit_memos",
                Long.class
        )).isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT additional_memo FROM property_pre_visit_memos WHERE property_id = 9102",
                String.class
        )).isEmpty();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT DATE_FORMAT(saved_at, '%Y-%m-%d %H:%i:%s.%f') "
                        + "FROM property_pre_visit_memos WHERE property_id = 9102",
                String.class
        )).isEqualTo("2026-08-10 04:30:00.333333");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT member_id FROM property_pre_visit_memos WHERE property_id = 9101",
                Long.class
        )).isEqualTo(9001L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM checklist_items WHERE origin = 'PROVIDED' AND id > 0",
                Long.class
        )).isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM visit_check_items visit_item
                JOIN checklist_items checklist_item
                  ON checklist_item.id = visit_item.source_checklist_item_id
                 AND checklist_item.check_item_id = visit_item.source_check_item_id
                WHERE visit_item.origin = 'PROVIDED'
                """, Long.class)).isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT inline_memo FROM visit_check_items WHERE id = 9601",
                String.class
        )).isEmpty();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT memo_version FROM visit_check_items WHERE id = 9601",
                Long.class
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT DATE_FORMAT(status_saved_at, '%Y-%m-%d %H:%i:%s.%f') "
                        + "FROM visit_check_items WHERE id = 9602",
                String.class
        )).isEqualTo("2026-08-10 02:20:00.222222");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT is_active FROM checklist_presets WHERE id = 4",
                Boolean.class
        )).isFalse();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT question FROM check_items WHERE id = 101",
                String.class
        )).isEqualTo("보일러 상태는 괜찮은가?");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT question_snapshot FROM visit_check_items WHERE id = 9602",
                String.class
        )).isEqualTo("과거 수압 질문");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE type = 'BASELINE' AND version = '1'",
                Long.class
        )).isEqualTo(1L);
        assertThat(successfulHistoryCount()).isEqualTo(4L);
    }

    private void assertExpandedConstraintsRejectInvalidRows() {
        jdbcTemplate.update("""
                INSERT INTO checklist_items (
                    checklist_id, origin, check_item_id, custom_question, stage, item_order
                ) VALUES (9301, 'CUSTOM', NULL, '엘리베이터 대기 시간은 괜찮은가?', 'ON_SITE', 3)
                """);
        final Long customItemId = jdbcTemplate.queryForObject(
                "SELECT id FROM checklist_items WHERE checklist_id = 9301 AND origin = 'CUSTOM'",
                Long.class
        );
        assertThat(customItemId).isPositive();
        jdbcTemplate.update(
                "UPDATE checklist_items SET custom_question = ? WHERE id = ?",
                "가".repeat(200),
                customItemId
        );
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE checklist_items SET custom_question = ? WHERE id = ?",
                "가".repeat(201),
                customItemId
        )).isInstanceOf(DataAccessException.class);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO checklist_items (
                    checklist_id, origin, check_item_id, custom_question, stage, item_order
                ) VALUES (9301, 'PROVIDED', NULL, NULL, 'ON_SITE', 4)
                """)).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO checklist_items (
                    checklist_id, origin, check_item_id, custom_question, stage, item_order
                ) VALUES (9301, 'CUSTOM', 102, '잘못된 사용자 질문', 'ON_SITE', 4)
                """)).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO checklist_items (
                    checklist_id, origin, check_item_id, custom_question, stage, item_order
                ) VALUES (9301, 'UNKNOWN', NULL, '잘못된 출처', 'ON_SITE', 4)
                """)).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO checklist_items (
                    checklist_id, origin, check_item_id, custom_question, stage, item_order
                ) VALUES (9301, 'PROVIDED', 102, NULL, 'ON_SITE', 1)
                """)).isInstanceOf(DataAccessException.class);

        jdbcTemplate.update("""
                INSERT INTO visit_check_items (
                    visit_stage_snapshot_id, stage, origin, source_checklist_item_id,
                    source_check_item_id, question_snapshot, guide_snapshot, item_order,
                    status, version, status_saved_at, inline_memo, memo_version,
                    memo_updated_at, created_at, updated_at
                ) VALUES (9501, 'ON_SITE', 'CUSTOM', ?, NULL, '엘리베이터 대기 시간은 괜찮은가?',
                    NULL, 3, 'UNCONFIRMED', 0, NULL, '', 0, NULL,
                    '2026-08-10 03:00:00.000000', '2026-08-10 03:00:00.000000')
                """, customItemId);
        jdbcTemplate.update(
                "UPDATE visit_check_items SET inline_memo = ?, memo_version = 1 WHERE item_order = 3",
                "메".repeat(200)
        );
        assertThat(jdbcTemplate.queryForObject(
                "SELECT CHAR_LENGTH(inline_memo) FROM visit_check_items WHERE item_order = 3",
                Integer.class
        )).isEqualTo(200);
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE visit_check_items SET inline_memo = ? WHERE item_order = 3",
                "메".repeat(201)
        )).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE visit_check_items SET memo_version = -1 WHERE item_order = 3"
        )).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO visit_check_items (
                    visit_stage_snapshot_id, stage, origin, source_checklist_item_id,
                    source_check_item_id, question_snapshot, guide_snapshot, item_order,
                    status, version, status_saved_at, inline_memo, memo_version,
                    memo_updated_at, created_at, updated_at
                ) VALUES (9501, 'ON_SITE', 'CUSTOM', NULL, NULL, '줄바꿈 메모 검사',
                    NULL, 4, 'UNCONFIRMED', 0, NULL, '첫 줄\n둘째 줄', 0, NULL,
                    '2026-08-10 03:00:00.000000', '2026-08-10 03:00:00.000000')
                """)).isInstanceOf(DataAccessException.class);
    }

    private long successfulHistoryCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE",
                Long.class
        );
    }

    private Connection rootConnection() throws SQLException {
        return DriverManager.getConnection(
                "jdbc:mysql://%s:%d/?allowPublicKeyRetrieval=true&useSSL=false"
                        .formatted(MYSQL_CONTAINER.getHost(), MYSQL_CONTAINER.getMappedPort(3306)),
                "root",
                MYSQL_CONTAINER.getPassword()
        );
    }

    private String databaseUrl() {
        return "jdbc:mysql://%s:%d/%s?allowPublicKeyRetrieval=true&useSSL=false"
                .formatted(MYSQL_CONTAINER.getHost(), MYSQL_CONTAINER.getMappedPort(3306), databaseName);
    }
}
