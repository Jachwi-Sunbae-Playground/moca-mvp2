package com.jachwisunbae.visit.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jachwisunbae.common.RepositoryTest;
import com.jachwisunbae.visit.domain.CheckStatus;
import com.jachwisunbae.visit.domain.InlineMemo;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

class VisitRepositoryTest extends RepositoryTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private VisitCheckItemRepository visitCheckItemRepository;

    @BeforeEach
    void setUp() {
        visitCheckItemRepository = new VisitCheckItemRepository(jdbcTemplate);
        jdbcTemplate.update("DELETE FROM properties");
        jdbcTemplate.update("DELETE FROM checklists");
        jdbcTemplate.update("DELETE FROM members");
    }

    @DisplayName("방문 세 테이블은 최종 FK·UNIQUE·CHECK·인덱스를 실제 MySQL에 생성한다")
    @Test
    void createVisitSchemaConstraintsAndIndexes() {
        final List<String> constraints = jdbcTemplate.queryForList(
                """
                SELECT CONSTRAINT_NAME
                FROM information_schema.TABLE_CONSTRAINTS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME IN ('visits', 'visit_stage_snapshots', 'visit_check_items')
                """,
                String.class
        );
        final List<String> indexes = jdbcTemplate.queryForList(
                """
                SELECT DISTINCT INDEX_NAME
                FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME IN ('visits', 'visit_stage_snapshots', 'visit_check_items')
                """,
                String.class
        );

        assertThat(constraints).contains(
                "fk_visits_property_owner",
                "ck_visits_status",
                "ck_visits_completion",
                "uk_visit_snapshots_visit_stage",
                "fk_visit_snapshots_source_checklist",
                "uk_visit_items_source",
                "uk_visit_items_source_checklist_item",
                "uk_visit_items_order",
                "fk_visit_items_snapshot_stage",
                "fk_visit_items_source_checklist_item",
                "fk_visit_items_source_stage",
                "ck_visit_items_origin",
                "ck_visit_items_source",
                "ck_visit_items_status",
                "ck_visit_items_version",
                "ck_visit_items_memo_version",
                "ck_visit_items_inline_memo"
        );
        assertThat(indexes).contains(
                "idx_visits_property_started",
                "idx_visits_member_started",
                "uk_visit_snapshots_visit_stage",
                "uk_visit_items_order"
        );
    }

    @DisplayName("같은 매물에 여러 방문을 저장하고 원본 체크리스트 삭제와 매물 cascade 정책을 지킨다")
    @Test
    void preserveSnapshotAndCascadeProperty() {
        final long memberId = saveMember("visit-schema-owner");
        final long propertyId = saveProperty(memberId);
        final long checklistId = saveChecklist(memberId);
        final long firstVisitId = saveVisit(memberId, propertyId);
        final long secondVisitId = saveVisit(memberId, propertyId);
        final long snapshotId = saveSnapshot(firstVisitId, checklistId);
        saveVisitItem(snapshotId);

        assertThat(firstVisitId).isNotEqualTo(secondVisitId);
        jdbcTemplate.update("DELETE FROM checklists WHERE id = ?", checklistId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT source_checklist_id FROM visit_stage_snapshots WHERE id = ?",
                Long.class,
                snapshotId
        )).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT question_snapshot FROM visit_check_items WHERE visit_stage_snapshot_id = ?",
                String.class,
                snapshotId
        )).isEqualTo("스냅샷 질문");

        jdbcTemplate.update("DELETE FROM properties WHERE id = ? AND member_id = ?", propertyId, memberId);

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM visits", Long.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM visit_stage_snapshots", Long.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM visit_check_items", Long.class)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM check_items WHERE id = 101",
                Long.class
        )).isOne();
    }

    @DisplayName("방문 상태·완료 시각과 항목 상태·버전 무결성을 DB가 거부한다")
    @Test
    void rejectInvalidVisitAndItemStates() {
        final long memberId = saveMember("visit-constraint-owner");
        final long propertyId = saveProperty(memberId);
        final long visitId = saveVisit(memberId, propertyId);
        final long snapshotId = saveSnapshot(visitId, null);

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO visits (property_id, member_id, status, started_at, completed_at, updated_at)
                VALUES (?, ?, 'COMPLETED', CURRENT_TIMESTAMP(6), NULL, CURRENT_TIMESTAMP(6))
                """,
                propertyId,
                memberId
        )).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO visit_check_items (
                    visit_stage_snapshot_id, stage, source_check_item_id,
                    question_snapshot, item_order, status, version
                ) VALUES (?, 'ON_SITE', 101, '질문', 1, 'BAD', 0)
                """,
                snapshotId
        )).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO visit_check_items (
                    visit_stage_snapshot_id, stage, source_check_item_id,
                    question_snapshot, item_order, status, version
                ) VALUES (?, 'ON_SITE', 101, '질문', 1, 'GOOD', -1)
                """,
                snapshotId
        )).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO visit_check_items (
                    visit_stage_snapshot_id, stage, source_check_item_id,
                    question_snapshot, item_order, status, version, inline_memo, memo_version
                ) VALUES (?, 'ON_SITE', 101, '질문', 2, 'GOOD', 0, '개행\n메모', 0)
                """,
                snapshotId
        )).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO visit_check_items (
                    visit_stage_snapshot_id, stage, source_check_item_id,
                    question_snapshot, item_order, status, version, inline_memo, memo_version
                ) VALUES (?, 'ON_SITE', 101, '질문', 3, 'GOOD', 0, '', -1)
                """,
                snapshotId
        )).isInstanceOf(DataAccessException.class);
    }

    @DisplayName("상태와 메모 조건부 UPDATE는 소유권과 각 버전을 검사하고 반대 채널을 변경하지 않는다")
    @Test
    void updateStatusAndMemoWithIndependentCas() {
        final long memberId = saveMember("visit-cas-owner");
        final long propertyId = saveProperty(memberId);
        final long visitId = saveVisit(memberId, propertyId);
        final long snapshotId = saveSnapshot(visitId, null);
        saveVisitItem(snapshotId);
        final long itemId = jdbcTemplate.queryForObject(
                "SELECT id FROM visit_check_items WHERE visit_stage_snapshot_id = ?",
                Long.class,
                snapshotId
        );
        final Instant statusSavedAt = Instant.parse("2026-08-12T02:00:00Z");
        final Instant memoSavedAt = Instant.parse("2026-08-12T02:00:01Z");

        assertThat(visitCheckItemRepository.updateStatus(
                memberId,
                visitId,
                itemId,
                CheckStatus.GOOD,
                0,
                statusSavedAt
        )).isTrue();
        assertThat(visitCheckItemRepository.updateStatus(
                memberId,
                visitId,
                itemId,
                CheckStatus.CAUTION,
                0,
                statusSavedAt
        )).isFalse();
        final var afterStatus = visitCheckItemRepository.findOwnedMemo(memberId, visitId, itemId).orElseThrow();
        assertThat(afterStatus.memo().value()).isEmpty();
        assertThat(afterStatus.memoVersion()).isZero();
        assertThat(afterStatus.memoSavedAt()).isNull();

        assertThat(visitCheckItemRepository.updateMemo(
                memberId,
                visitId,
                itemId,
                new InlineMemo("독립 메모"),
                0,
                memoSavedAt
        )).isTrue();
        assertThat(visitCheckItemRepository.updateMemo(
                memberId + 1,
                visitId,
                itemId,
                new InlineMemo("타인 메모"),
                1,
                memoSavedAt
        )).isFalse();

        final var status = visitCheckItemRepository.findOwnedStatus(memberId, visitId, itemId).orElseThrow();
        final var memo = visitCheckItemRepository.findOwnedMemo(memberId, visitId, itemId).orElseThrow();
        assertThat(status.status()).isEqualTo(CheckStatus.GOOD);
        assertThat(status.statusVersion()).isOne();
        assertThat(status.statusSavedAt()).isEqualTo(statusSavedAt);
        assertThat(memo.memo().value()).isEqualTo("독립 메모");
        assertThat(memo.memoVersion()).isOne();
        assertThat(memo.memoSavedAt()).isEqualTo(memoSavedAt);
    }

    @DisplayName("방문 목록 집계와 매물 최근 방문 조회는 시작 시각 복합 인덱스를 사용한다")
    @Test
    void explainVisitListIndex() {
        final long memberId = saveMember("visit-explain-owner");
        final long propertyId = saveProperty(memberId);
        final long visitId = saveVisit(memberId, propertyId);
        final long snapshotId = saveSnapshot(visitId, null);
        saveVisitItem(snapshotId);

        final List<Map<String, Object>> listPlan = jdbcTemplate.queryForList(
                """
                EXPLAIN SELECT v.id,
                               v.status,
                               v.started_at,
                               COUNT(vci.id) AS total_count
                FROM visits v
                JOIN visit_stage_snapshots vss ON vss.visit_id = v.id
                JOIN visit_check_items vci ON vci.visit_stage_snapshot_id = vss.id
                WHERE v.property_id = ?
                  AND v.member_id = ?
                GROUP BY v.id, v.status, v.started_at
                ORDER BY v.started_at DESC, v.id DESC
                LIMIT 20
                """,
                propertyId,
                memberId
        );
        final List<Map<String, Object>> recentPlan = jdbcTemplate.queryForList(
                """
                EXPLAIN SELECT p.id, recent.id
                FROM properties p
                LEFT JOIN visits recent ON recent.id = (
                    SELECT candidate.id
                    FROM visits candidate
                    WHERE candidate.property_id = p.id
                    ORDER BY candidate.started_at DESC, candidate.id DESC
                    LIMIT 1
                )
                WHERE p.id = ?
                  AND p.member_id = ?
                """,
                propertyId,
                memberId
        );

        assertThat(listPlan).anyMatch(row -> hasVisitStartedIndex(row));
        assertThat(recentPlan).anyMatch(row -> hasVisitStartedIndex(row));
    }

    private boolean hasVisitStartedIndex(final Map<String, Object> row) {
        final String selectedKey = String.valueOf(row.get("key"));
        final String possibleKeys = String.valueOf(row.get("possible_keys"));
        return selectedKey.contains("idx_visits_property_started")
                || possibleKeys.contains("idx_visits_property_started");
    }

    private long saveMember(final String subject) {
        jdbcTemplate.update(
                """
                INSERT INTO members (oauth_provider, oauth_subject, email, display_name, last_login_at)
                VALUES ('GOOGLE', ?, ?, '회원', CURRENT_TIMESTAMP(6))
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

    private long saveProperty(final long memberId) {
        jdbcTemplate.update(
                """
                INSERT INTO properties (
                    member_id, name, deposit_amount, monthly_rent_amount,
                    discovery_source_type, discovery_source, last_activity_at
                ) VALUES (?, '매물', 0, 0, 'TEXT', '직접', CURRENT_TIMESTAMP(6))
                """,
                memberId
        );
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM properties", Long.class);
    }

    private long saveChecklist(final long memberId) {
        jdbcTemplate.update(
                """
                INSERT INTO checklists (member_id, name, stage, created_at, updated_at)
                VALUES (?, '현장', 'ON_SITE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """,
                memberId
        );
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM checklists", Long.class);
    }

    private long saveVisit(final long memberId, final long propertyId) {
        jdbcTemplate.update(
                """
                INSERT INTO visits (property_id, member_id, status, started_at, completed_at, updated_at)
                VALUES (?, ?, 'IN_PROGRESS', CURRENT_TIMESTAMP(6), NULL, CURRENT_TIMESTAMP(6))
                """,
                propertyId,
                memberId
        );
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM visits", Long.class);
    }

    private long saveSnapshot(final long visitId, final Long checklistId) {
        jdbcTemplate.update(
                """
                INSERT INTO visit_stage_snapshots (
                    visit_id, stage, source_checklist_id, checklist_name, created_at
                ) VALUES (?, 'ON_SITE', ?, '현장', CURRENT_TIMESTAMP(6))
                """,
                visitId,
                checklistId
        );
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM visit_stage_snapshots", Long.class);
    }

    private void saveVisitItem(final long snapshotId) {
        jdbcTemplate.update(
                """
                INSERT INTO visit_check_items (
                    visit_stage_snapshot_id, stage, source_check_item_id,
                    question_snapshot, item_order, status, version
                ) VALUES (?, 'ON_SITE', 101, '스냅샷 질문', 1, 'UNCONFIRMED', 0)
                """,
                snapshotId
        );
    }
}
