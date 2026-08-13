package com.jachwisunbae.property.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.common.RepositoryTest;
import com.jachwisunbae.property.domain.ActiveChecklist;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

class ActiveChecklistRepositoryTest extends RepositoryTest {

    private static final Instant NOW = Instant.parse("2026-08-11T00:00:00Z");

    private ActiveChecklistRepository activeChecklistRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long memberId;
    private long propertyId;
    private long secondPropertyId;
    private long checklistId;
    private long replacementChecklistId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM checklists");
        jdbcTemplate.update("DELETE FROM properties");
        jdbcTemplate.update("DELETE FROM members");
        activeChecklistRepository = new ActiveChecklistRepository(jdbcTemplate);
        memberId = saveMember("active-repository-owner");
        propertyId = saveProperty(memberId, "첫 매물");
        secondPropertyId = saveProperty(memberId, "둘째 매물");
        checklistId = saveChecklist(memberId, "현장 기본", "ON_SITE");
        replacementChecklistId = saveChecklist(memberId, "현장 교체", "ON_SITE");
    }

    @DisplayName("매물·단계별 하나를 upsert하고 같은 체크리스트를 여러 매물에서 사용한다")
    @Test
    void upsertPerPropertyAndStage() {
        assertThat(activeChecklistRepository.upsertOwned(active(propertyId, checklistId))).isTrue();
        assertThat(activeChecklistRepository.upsertOwned(active(secondPropertyId, checklistId))).isTrue();
        assertThat(activeChecklistRepository.upsertOwned(active(propertyId, replacementChecklistId))).isTrue();

        assertThat(activeChecklistRepository.findOwned(memberId, propertyId, CheckStage.ON_SITE))
                .get()
                .extracting(ActiveChecklist::checklistId)
                .isEqualTo(replacementChecklistId);
        assertThat(activeChecklistRepository.findOwned(memberId, secondPropertyId, CheckStage.ON_SITE))
                .get()
                .extracting(ActiveChecklist::checklistId)
                .isEqualTo(checklistId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM property_active_checklists",
                Long.class
        )).isEqualTo(2L);
    }

    @DisplayName("활성 체크리스트 요약은 source ID가 없는 CUSTOM도 실제 항목 수에 포함한다")
    @Test
    void countCustomItemsInActiveChecklistSummary() {
        jdbcTemplate.update(
                """
                INSERT INTO checklist_items (
                    checklist_id, origin, check_item_id, custom_question, stage, item_order
                ) VALUES (?, 'CUSTOM', NULL, '창틀 곰팡이는 괜찮은가?', 'ON_SITE', 2)
                """,
                checklistId
        );
        activeChecklistRepository.upsertOwned(active(propertyId, checklistId));

        assertThat(activeChecklistRepository.findAllOwned(memberId, propertyId))
                .singleElement()
                .extracting(ActiveChecklistProjection::itemCount)
                .isEqualTo(2);
    }

    @DisplayName("복합 FK와 CHECK가 매물·체크리스트 소유자와 단계를 강제한다")
    @Test
    void enforceOwnerAndStageConstraints() {
        final long otherMemberId = saveMember("active-repository-other");
        final long otherChecklistId = saveChecklist(otherMemberId, "타인 현장", "ON_SITE");

        assertThatThrownBy(() -> insertRaw(propertyId, memberId, "ON_SITE", otherChecklistId))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> insertRaw(propertyId, memberId, "ONLINE_PHONE", checklistId))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> insertRaw(propertyId, memberId, "INVALID", checklistId))
                .isInstanceOf(DataAccessException.class);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM property_active_checklists",
                Long.class
        )).isZero();
    }

    @DisplayName("매물과 체크리스트 삭제는 활성 연결만 cascade하고 반대편 루트는 유지한다")
    @Test
    void cascadeOnlyConnections() {
        activeChecklistRepository.upsertOwned(active(propertyId, checklistId));
        activeChecklistRepository.upsertOwned(active(secondPropertyId, checklistId));

        jdbcTemplate.update("DELETE FROM properties WHERE id = ?", propertyId);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM property_active_checklists WHERE checklist_id = ?",
                Long.class,
                checklistId
        )).isOne();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM checklists WHERE id = ?",
                Long.class,
                checklistId
        )).isOne();

        jdbcTemplate.update("DELETE FROM checklists WHERE id = ?", checklistId);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM property_active_checklists",
                Long.class
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM properties WHERE id = ?",
                Long.class,
                secondPropertyId
        )).isOne();
    }

    @DisplayName("실제 MySQL에 단계별 PK·복합 FK·CHECK·집계 인덱스가 생성된다")
    @Test
    void createRequiredConstraintsAndIndex() {
        final List<String> constraints = jdbcTemplate.queryForList(
                """
                SELECT CONSTRAINT_NAME
                FROM information_schema.TABLE_CONSTRAINTS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'property_active_checklists'
                """,
                String.class
        );
        final List<String> indexes = jdbcTemplate.queryForList(
                """
                SELECT DISTINCT INDEX_NAME
                FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'property_active_checklists'
                """,
                String.class
        );

        assertThat(constraints).contains(
                "PRIMARY",
                "fk_active_checklists_property_owner",
                "fk_active_checklists_checklist_owner_stage",
                "ck_active_checklists_stage"
        );
        assertThat(indexes).contains("PRIMARY", "idx_active_checklists_checklist");
    }

    @DisplayName("체크리스트 목록의 할당 수 상관 집계는 checklist_id 선두 인덱스를 사용한다")
    @Test
    void explainAssignedPropertyCountQuery() {
        activeChecklistRepository.upsertOwned(active(propertyId, checklistId));
        activeChecklistRepository.upsertOwned(active(secondPropertyId, checklistId));

        final List<Map<String, Object>> plan = jdbcTemplate.queryForList(
                """
                EXPLAIN
                SELECT checklist.id,
                       (SELECT COUNT(*)
                        FROM property_active_checklists active
                        WHERE active.checklist_id = checklist.id) AS assigned_property_count
                FROM checklists checklist
                WHERE checklist.member_id = ?
                  AND checklist.stage = 'ON_SITE'
                ORDER BY checklist.updated_at DESC, checklist.id DESC
                LIMIT 20
                """,
                memberId
        );

        assertThat(plan).anySatisfy(row -> {
            assertThat(row.get("table")).isEqualTo("active");
            assertThat(row.get("type")).isEqualTo("ref");
            assertThat(row.get("key") + "," + row.get("possible_keys"))
                    .contains("idx_active_checklists_checklist");
        });
    }

    private ActiveChecklist active(final long targetPropertyId, final long targetChecklistId) {
        return ActiveChecklist.create(
                targetPropertyId,
                memberId,
                CheckStage.ON_SITE,
                targetChecklistId,
                NOW
        );
    }

    private void insertRaw(
            final long targetPropertyId,
            final long ownerId,
            final String stage,
            final long targetChecklistId
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO property_active_checklists (
                    property_id, member_id, stage, checklist_id, created_at, updated_at
                ) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """,
                targetPropertyId,
                ownerId,
                stage,
                targetChecklistId
        );
    }

    private long saveMember(final String subject) {
        jdbcTemplate.update(
                """
                INSERT INTO members (
                    oauth_provider, oauth_subject, email, display_name, last_login_at
                ) VALUES ('GOOGLE', ?, ?, '회원', CURRENT_TIMESTAMP(6))
                """,
                subject,
                subject + "@example.com"
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM members WHERE oauth_subject = ?",
                Long.class,
                subject
        );
    }

    private long saveProperty(final long ownerId, final String name) {
        jdbcTemplate.update(
                """
                INSERT INTO properties (
                    member_id, name, deposit_amount, monthly_rent_amount,
                    discovery_source_type, discovery_source, last_activity_at
                ) VALUES (?, ?, 0, 0, 'TEXT', '직접 발견', CURRENT_TIMESTAMP(6))
                """,
                ownerId,
                name
        );
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM properties", Long.class);
    }

    private long saveChecklist(final long ownerId, final String name, final String stage) {
        jdbcTemplate.update(
                """
                INSERT INTO checklists (member_id, name, stage, created_at, updated_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """,
                ownerId,
                name,
                stage
        );
        final long id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM checklists", Long.class);
        final long itemId = switch (stage) {
            case "ONLINE_PHONE" -> 201L;
            case "ON_SITE" -> 101L;
            case "PRE_CONTRACT" -> 301L;
            default -> throw new IllegalArgumentException("지원하지 않는 단계입니다.");
        };
        jdbcTemplate.update(
                """
                INSERT INTO checklist_items (checklist_id, check_item_id, stage, item_order)
                VALUES (?, ?, ?, 1)
                """,
                id,
                itemId,
                stage
        );
        return id;
    }
}
