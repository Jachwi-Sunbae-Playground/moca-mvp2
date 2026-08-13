package com.jachwisunbae.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class MvpDatabaseBaselineRepositoryTest extends RepositoryTest {

    private static final List<String> PRODUCT_TABLES = List.of(
            "members",
            "properties",
            "property_pre_visit_memos",
            "property_photos",
            "check_items",
            "checklist_presets",
            "checklist_preset_items",
            "checklists",
            "checklist_items",
            "property_active_checklists",
            "visits",
            "visit_stage_snapshots",
            "visit_check_items"
    );
    private static final Map<String, Integer> IMMUTABLE_MIGRATION_CHECKSUMS = Map.of(
            "1", 1_085_074_436,
            "2", 139_873_113,
            "3", -669_862_584,
            "4", 1_908_786_006
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DisplayName("Flyway는 정확히 13개 제품 테이블과 72개 기준 항목·6개 프리셋을 초기화한다")
    @Test
    void createExactProductSchemaAndSeedData() {
        final List<String> tables = jdbcTemplate.queryForList(
                """
                SELECT TABLE_NAME
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_TYPE = 'BASE TABLE'
                  AND TABLE_NAME <> 'flyway_schema_history'
                """,
                String.class
        );

        assertThat(tables).containsExactlyInAnyOrderElementsOf(PRODUCT_TABLES);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM check_items", Long.class)).isEqualTo(72L);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM checklist_presets", Long.class)).isEqualTo(6L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM checklist_presets WHERE is_active = TRUE",
                Long.class
        )).isEqualTo(3L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE",
                Long.class
        )).isEqualTo(4L);
        final Map<String, Integer> migrationChecksums = jdbcTemplate.query(
                "SELECT version, checksum FROM flyway_schema_history WHERE success = TRUE ORDER BY installed_rank",
                resultSet -> {
                    final Map<String, Integer> checksums = new LinkedHashMap<>();
                    while (resultSet.next()) {
                        checksums.put(resultSet.getString("version"), resultSet.getInt("checksum"));
                    }
                    return checksums;
                }
        );
        assertThat(migrationChecksums).containsExactlyInAnyOrderEntriesOf(IMMUTABLE_MIGRATION_CHECKSUMS);
    }

    @DisplayName("최종 소유권·단계·삭제 제약과 핵심 정렬 인덱스가 실제 MySQL에 존재한다")
    @Test
    void createCriticalConstraintsAndIndexes() {
        final List<String> constraints = jdbcTemplate.queryForList(
                """
                SELECT CONSTRAINT_NAME
                FROM information_schema.TABLE_CONSTRAINTS
                WHERE TABLE_SCHEMA = DATABASE()
                """,
                String.class
        );
        final List<String> indexes = jdbcTemplate.queryForList(
                """
                SELECT DISTINCT INDEX_NAME
                FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                """,
                String.class
        );
        final Map<String, String> deleteRules = jdbcTemplate.query(
                """
                SELECT CONSTRAINT_NAME, DELETE_RULE
                FROM information_schema.REFERENTIAL_CONSTRAINTS
                WHERE CONSTRAINT_SCHEMA = DATABASE()
                """,
                resultSet -> {
                    final java.util.HashMap<String, String> rules = new java.util.HashMap<>();
                    while (resultSet.next()) {
                        rules.put(resultSet.getString("CONSTRAINT_NAME"), resultSet.getString("DELETE_RULE"));
                    }
                    return rules;
                }
        );

        assertThat(constraints).contains(
                "uk_members_provider_subject",
                "fk_property_photos_property_owner",
                "fk_pre_visit_memos_property_owner",
                "fk_checklist_items_checklist_stage",
                "fk_checklist_items_check_item_stage",
                "uk_checklist_items_provided",
                "ck_checklist_items_origin",
                "ck_checklist_items_source",
                "fk_active_checklists_property_owner",
                "fk_active_checklists_checklist_owner_stage",
                "fk_visits_property_owner",
                "fk_visit_items_snapshot_stage",
                "fk_visit_items_source_stage",
                "fk_visit_items_source_checklist_item",
                "uk_visit_items_source",
                "uk_visit_items_source_checklist_item",
                "ck_visit_items_origin",
                "ck_visit_items_source",
                "ck_visit_items_version",
                "ck_visit_items_memo_version",
                "ck_visit_items_inline_memo"
        );
        assertThat(indexes).contains(
                "idx_properties_member_activity",
                "idx_property_photos_property_created",
                "idx_checklists_member_stage_updated",
                "idx_active_checklists_checklist",
                "idx_visits_property_started"
        );
        assertThat(deleteRules).containsEntry("fk_property_photos_property_owner", "CASCADE")
                .containsEntry("fk_pre_visit_memos_property_owner", "CASCADE")
                .containsEntry("fk_active_checklists_property_owner", "CASCADE")
                .containsEntry("fk_visits_property_owner", "CASCADE")
                .containsEntry("fk_visit_snapshots_source_checklist", "SET NULL")
                .containsEntry("fk_visit_items_source_checklist_item", "SET NULL")
                .containsEntry("fk_checklist_items_check_item_stage", "RESTRICT");
        assertThat(deleteRules.get("fk_properties_member")).isNotEqualTo("CASCADE");
        assertThat(deleteRules.get("fk_checklists_member")).isNotEqualTo("CASCADE");
    }

    @DisplayName("비범위 인프라는 없고 기존 상태 버전과 새 메모 버전을 함께 유지한다")
    @Test
    void keepProhibitedSchemaAbsent() {
        final List<String> prohibitedTables = List.of(
                "spring_session",
                "spring_session_attributes",
                "refresh_tokens",
                "outbox_events",
                "idempotency_keys",
                "file_deletion_jobs"
        );
        final List<String> existingProhibitedTables = jdbcTemplate.queryForList(
                """
                SELECT TABLE_NAME
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                  AND LOWER(TABLE_NAME) IN (?, ?, ?, ?, ?, ?)
                """,
                String.class,
                prohibitedTables.toArray()
        );
        final List<String> propertyColumns = columnsOf("properties");
        final List<String> checklistItemColumns = columnsOf("checklist_items");
        final List<String> visitItemColumns = columnsOf("visit_check_items");

        assertThat(existingProhibitedTables).isEmpty();
        assertThat(propertyColumns).doesNotContain("memo_version");
        assertThat(checklistItemColumns).contains("id", "origin", "custom_question");
        assertThat(visitItemColumns).contains(
                "version",
                "status_saved_at",
                "inline_memo",
                "memo_version",
                "memo_updated_at"
        );
    }

    private List<String> columnsOf(final String tableName) {
        return jdbcTemplate.queryForList(
                """
                SELECT COLUMN_NAME
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                """,
                String.class,
                tableName
        );
    }
}
