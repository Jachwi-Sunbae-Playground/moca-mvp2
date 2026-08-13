package com.jachwisunbae.visit.repository;

import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.exception.server.DataInconsistencyException;
import com.jachwisunbae.visit.domain.CheckStatus;
import com.jachwisunbae.visit.domain.InlineMemo;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class VisitCheckItemRepository {

    private static final String UPDATE_STATUS_SQL = """
            UPDATE visit_check_items vci
            SET vci.status = ?,
                vci.version = vci.version + 1,
                vci.status_saved_at = ?,
                vci.updated_at = ?
            WHERE vci.id = ?
              AND vci.version = ?
              AND EXISTS (
                  SELECT 1
                  FROM visit_stage_snapshots vss
                  JOIN visits v ON v.id = vss.visit_id
                  WHERE vss.id = vci.visit_stage_snapshot_id
                    AND v.id = ?
                    AND v.member_id = ?
              )
            """;
    private static final String UPDATE_MEMO_SQL = """
            UPDATE visit_check_items vci
            SET vci.inline_memo = ?,
                vci.memo_version = vci.memo_version + 1,
                vci.memo_updated_at = ?,
                vci.updated_at = ?
            WHERE vci.id = ?
              AND vci.memo_version = ?
              AND EXISTS (
                  SELECT 1
                  FROM visit_stage_snapshots vss
                  JOIN visits v ON v.id = vss.visit_id
                  WHERE vss.id = vci.visit_stage_snapshot_id
                    AND v.id = ?
                    AND v.member_id = ?
              )
            """;
    private static final String FIND_OWNED_STATUS_SQL = """
            SELECT vci.id,
                   vss.stage,
                   vci.status,
                   vci.version,
                   COALESCE(vci.status_saved_at, vci.updated_at) AS status_saved_at
            FROM visit_check_items vci
            JOIN visit_stage_snapshots vss ON vss.id = vci.visit_stage_snapshot_id
            JOIN visits v ON v.id = vss.visit_id
            WHERE v.id = ?
              AND v.member_id = ?
              AND vci.id = ?
            """;
    private static final String FIND_OWNED_MEMO_SQL = """
            SELECT vci.id, vci.inline_memo, vci.memo_version, vci.memo_updated_at
            FROM visit_check_items vci
            JOIN visit_stage_snapshots vss ON vss.id = vci.visit_stage_snapshot_id
            JOIN visits v ON v.id = vss.visit_id
            WHERE v.id = ?
              AND v.member_id = ?
              AND vci.id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public VisitCheckItemRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean updateStatus(
            final long memberId,
            final long visitId,
            final long visitItemId,
            final CheckStatus status,
            final long expectedVersion,
            final Instant savedAt
    ) {
        try {
            return jdbcTemplate.update(
                    UPDATE_STATUS_SQL,
                    status.name(),
                    Timestamp.from(savedAt),
                    Timestamp.from(savedAt),
                    visitItemId,
                    expectedVersion,
                    visitId,
                    memberId
            ) == 1;
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public boolean updateMemo(
            final long memberId,
            final long visitId,
            final long visitItemId,
            final InlineMemo memo,
            final long expectedMemoVersion,
            final Instant savedAt
    ) {
        try {
            return jdbcTemplate.update(
                    UPDATE_MEMO_SQL,
                    memo.value(),
                    Timestamp.from(savedAt),
                    Timestamp.from(savedAt),
                    visitItemId,
                    expectedMemoVersion,
                    visitId,
                    memberId
            ) == 1;
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public Optional<VisitItemStatusStateProjection> findOwnedStatus(
            final long memberId,
            final long visitId,
            final long visitItemId
    ) {
        try {
            return jdbcTemplate.query(
                    FIND_OWNED_STATUS_SQL,
                    (resultSet, rowNumber) -> new VisitItemStatusStateProjection(
                            resultSet.getLong("id"),
                            CheckStage.valueOf(resultSet.getString("stage")),
                            CheckStatus.valueOf(resultSet.getString("status")),
                            resultSet.getLong("version"),
                            resultSet.getTimestamp("status_saved_at").toInstant()
                    ),
                    visitId,
                    memberId,
                    visitItemId
            ).stream().findFirst();
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public Optional<VisitItemMemoStateProjection> findOwnedMemo(
            final long memberId,
            final long visitId,
            final long visitItemId
    ) {
        try {
            return jdbcTemplate.query(
                    FIND_OWNED_MEMO_SQL,
                    (resultSet, rowNumber) -> {
                        final Timestamp memoSavedAt = resultSet.getTimestamp("memo_updated_at");
                        return new VisitItemMemoStateProjection(
                                resultSet.getLong("id"),
                                new InlineMemo(resultSet.getString("inline_memo")),
                                resultSet.getLong("memo_version"),
                                memoSavedAt == null ? null : memoSavedAt.toInstant()
                        );
                    },
                    visitId,
                    memberId,
                    visitItemId
            ).stream().findFirst();
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    private DataInconsistencyException dataInconsistency(final DataAccessException cause) {
        return new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR, cause);
    }
}
