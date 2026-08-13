package com.jachwisunbae.visit.repository;

import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.exception.server.DataInconsistencyException;
import com.jachwisunbae.common.page.PageQuery;
import com.jachwisunbae.visit.domain.CheckStatus;
import com.jachwisunbae.visit.domain.VisitStatus;
import com.jachwisunbae.visit.domain.VisitSummary;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class VisitQueryRepository {

    private static final String COUNT_OWNED_BY_PROPERTY_SQL = """
            SELECT COUNT(*)
            FROM visits
            WHERE property_id = ?
              AND member_id = ?
            """;
    private static final String FIND_ALL_OWNED_BY_PROPERTY_SQL = """
            SELECT v.id,
                   v.status,
                   v.started_at,
                   v.completed_at,
                   COUNT(vci.id) AS total_count,
                   SUM(CASE WHEN vci.status = 'GOOD' THEN 1 ELSE 0 END) AS good_count,
                   SUM(CASE WHEN vci.status = 'CAUTION' THEN 1 ELSE 0 END) AS caution_count,
                   SUM(CASE WHEN vci.status = 'UNCONFIRMED' THEN 1 ELSE 0 END) AS unconfirmed_count
            FROM visits v
            JOIN visit_stage_snapshots vss ON vss.visit_id = v.id
            JOIN visit_check_items vci ON vci.visit_stage_snapshot_id = vss.id
            WHERE v.property_id = ?
              AND v.member_id = ?
            GROUP BY v.id, v.status, v.started_at, v.completed_at
            ORDER BY v.started_at DESC, v.id DESC
            LIMIT ? OFFSET ?
            """;
    private static final String FIND_OWNED_DETAIL_SQL = """
            SELECT v.id AS visit_id,
                   v.property_id,
                   v.status AS visit_status,
                   v.started_at,
                   v.completed_at,
                   v.updated_at,
                   vss.id AS snapshot_id,
                   vss.stage,
                   vss.source_checklist_id,
                   vss.checklist_name,
                   vci.id AS visit_item_id,
                   vci.origin,
                   vci.source_checklist_item_id,
                   vci.source_check_item_id,
                   vci.question_snapshot,
                   vci.guide_snapshot,
                   vci.item_order,
                   vci.status AS item_status,
                   vci.version AS status_version,
                   COALESCE(vci.status_saved_at, vci.updated_at) AS status_saved_at,
                   vci.inline_memo,
                   vci.memo_version,
                   vci.memo_updated_at
            FROM visits v
            JOIN visit_stage_snapshots vss ON vss.visit_id = v.id
            JOIN visit_check_items vci ON vci.visit_stage_snapshot_id = vss.id
            WHERE v.id = ?
              AND v.member_id = ?
            ORDER BY CASE vss.stage
                         WHEN 'ONLINE_PHONE' THEN 1
                         WHEN 'ON_SITE' THEN 2
                         WHEN 'PRE_CONTRACT' THEN 3
                         ELSE 4
                     END,
                     vss.id,
                     vci.item_order
            """;
    private static final String FIND_SUMMARY_SQL = """
            SELECT COUNT(vci.id) AS total_count,
                   SUM(CASE WHEN vci.status = 'GOOD' THEN 1 ELSE 0 END) AS good_count,
                   SUM(CASE WHEN vci.status = 'CAUTION' THEN 1 ELSE 0 END) AS caution_count,
                   SUM(CASE WHEN vci.status = 'UNCONFIRMED' THEN 1 ELSE 0 END) AS unconfirmed_count
            FROM visit_stage_snapshots vss
            JOIN visit_check_items vci ON vci.visit_stage_snapshot_id = vss.id
            JOIN visits v ON v.id = vss.visit_id
            WHERE v.id = ?
              AND v.member_id = ?
            """;
    private static final String FIND_STAGE_SUMMARY_SQL = FIND_SUMMARY_SQL + " AND vss.stage = ?";

    private final JdbcTemplate jdbcTemplate;

    public VisitQueryRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long countOwnedByProperty(final long memberId, final long propertyId) {
        try {
            final Long count = jdbcTemplate.queryForObject(
                    COUNT_OWNED_BY_PROPERTY_SQL,
                    Long.class,
                    propertyId,
                    memberId
            );
            return count == null ? 0 : count;
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public List<VisitListProjection> findAllOwnedByProperty(
            final long memberId,
            final long propertyId,
            final PageQuery pageQuery
    ) {
        try {
            return jdbcTemplate.query(
                    FIND_ALL_OWNED_BY_PROPERTY_SQL,
                    (resultSet, rowNumber) -> {
                        final Timestamp completedAt = resultSet.getTimestamp("completed_at");
                        return new VisitListProjection(
                                resultSet.getLong("id"),
                                VisitStatus.valueOf(resultSet.getString("status")),
                                resultSet.getTimestamp("started_at").toInstant(),
                                completedAt == null ? null : completedAt.toInstant(),
                                summaryFrom(resultSet)
                        );
                    },
                    propertyId,
                    memberId,
                    pageQuery.size(),
                    pageQuery.offset()
            );
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public List<VisitDetailRow> findOwnedDetail(final long memberId, final long visitId) {
        try {
            return jdbcTemplate.query(
                    FIND_OWNED_DETAIL_SQL,
                    (resultSet, rowNumber) -> {
                        final Timestamp completedAt = resultSet.getTimestamp("completed_at");
                        final Timestamp memoSavedAt = resultSet.getTimestamp("memo_updated_at");
                        return new VisitDetailRow(
                                resultSet.getLong("visit_id"),
                                resultSet.getLong("property_id"),
                                VisitStatus.valueOf(resultSet.getString("visit_status")),
                                resultSet.getTimestamp("started_at").toInstant(),
                                completedAt == null ? null : completedAt.toInstant(),
                                resultSet.getTimestamp("updated_at").toInstant(),
                                resultSet.getLong("snapshot_id"),
                                CheckStage.valueOf(resultSet.getString("stage")),
                                resultSet.getObject("source_checklist_id", Long.class),
                                resultSet.getString("checklist_name"),
                                resultSet.getLong("visit_item_id"),
                                com.jachwisunbae.checklist.domain.ChecklistItemOrigin.valueOf(
                                        resultSet.getString("origin")
                                ),
                                resultSet.getObject("source_checklist_item_id", Long.class),
                                resultSet.getObject("source_check_item_id", Long.class),
                                resultSet.getString("question_snapshot"),
                                resultSet.getString("guide_snapshot"),
                                resultSet.getInt("item_order"),
                                CheckStatus.valueOf(resultSet.getString("item_status")),
                                resultSet.getLong("status_version"),
                                resultSet.getTimestamp("status_saved_at").toInstant(),
                                resultSet.getString("inline_memo"),
                                resultSet.getLong("memo_version"),
                                memoSavedAt == null ? null : memoSavedAt.toInstant()
                        );
                    },
                    visitId,
                    memberId
            );
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public Optional<VisitSummary> findSummary(final long memberId, final long visitId) {
        return findSummary(FIND_SUMMARY_SQL, visitId, memberId);
    }

    public Optional<VisitSummary> findStageSummary(
            final long memberId,
            final long visitId,
            final CheckStage stage
    ) {
        return findSummary(FIND_STAGE_SUMMARY_SQL, visitId, memberId, stage.name());
    }

    private Optional<VisitSummary> findSummary(final String sql, final Object... arguments) {
        try {
            return jdbcTemplate.query(sql, (resultSet, rowNumber) -> summaryFrom(resultSet), arguments)
                    .stream()
                    .filter(summary -> summary.totalCount() > 0)
                    .findFirst();
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    private static VisitSummary summaryFrom(final java.sql.ResultSet resultSet) throws java.sql.SQLException {
        return VisitSummary.from(
                resultSet.getInt("total_count"),
                resultSet.getInt("good_count"),
                resultSet.getInt("caution_count"),
                resultSet.getInt("unconfirmed_count")
        );
    }

    private DataInconsistencyException dataInconsistency(final DataAccessException cause) {
        return new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR, cause);
    }
}
