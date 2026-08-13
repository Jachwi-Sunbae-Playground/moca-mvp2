package com.jachwisunbae.visit.repository;

import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.exception.server.DataInconsistencyException;
import com.jachwisunbae.visit.domain.VisitCheckItem;
import com.jachwisunbae.visit.domain.VisitStageSnapshot;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class VisitSnapshotRepository {

    private static final String INSERT_STAGE_SQL = """
            INSERT INTO visit_stage_snapshots (
                visit_id, stage, source_checklist_id, checklist_name, created_at
            ) VALUES (?, ?, ?, ?, ?)
            """;
    private static final String INSERT_ITEM_SQL = """
            INSERT INTO visit_check_items (
                visit_stage_snapshot_id,
                stage,
                origin,
                source_checklist_item_id,
                source_check_item_id,
                question_snapshot,
                guide_snapshot,
                item_order,
                status,
                version,
                status_saved_at,
                inline_memo,
                memo_version,
                created_at,
                updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    public VisitSnapshotRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveAll(final List<VisitStageSnapshot> snapshots) {
        try {
            for (VisitStageSnapshot snapshot : snapshots) {
                final long snapshotId = insertStage(snapshot);
                insertItems(snapshotId, snapshot);
            }
        } catch (DataAccessException exception) {
            throw new DataInconsistencyException(ErrorCode.CHECKLIST_SNAPSHOT_FAILED, exception);
        }
    }

    private long insertStage(final VisitStageSnapshot snapshot) {
        final KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            final PreparedStatement statement = connection.prepareStatement(
                    INSERT_STAGE_SQL,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, snapshot.visitId());
            statement.setString(2, snapshot.stage().name());
            if (snapshot.sourceChecklistId() == null) {
                statement.setObject(3, null);
            } else {
                statement.setLong(3, snapshot.sourceChecklistId());
            }
            statement.setString(4, snapshot.checklistName());
            statement.setTimestamp(5, Timestamp.from(snapshot.createdAt()));
            return statement;
        }, keyHolder);
        final Number key = keyHolder.getKey();
        if (key == null) {
            throw new DataInconsistencyException(ErrorCode.CHECKLIST_SNAPSHOT_FAILED);
        }
        return key.longValue();
    }

    private void insertItems(final long snapshotId, final VisitStageSnapshot snapshot) {
        final List<VisitCheckItem> items = snapshot.items();
        jdbcTemplate.batchUpdate(INSERT_ITEM_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(final PreparedStatement statement, final int index) throws java.sql.SQLException {
                final VisitCheckItem item = items.get(index);
                statement.setLong(1, snapshotId);
                statement.setString(2, snapshot.stage().name());
                statement.setString(3, item.origin().name());
                if (item.sourceChecklistItemId() == null) {
                    statement.setObject(4, null);
                } else {
                    statement.setLong(4, item.sourceChecklistItemId());
                }
                if (item.sourceCheckItemId() == null) {
                    statement.setObject(5, null);
                } else {
                    statement.setLong(5, item.sourceCheckItemId());
                }
                statement.setString(6, item.question());
                statement.setString(7, item.guide());
                statement.setInt(8, item.order());
                statement.setString(9, item.status().name());
                statement.setLong(10, item.statusVersion());
                statement.setTimestamp(11, Timestamp.from(item.statusSavedAt()));
                statement.setString(12, item.inlineMemo().value());
                statement.setLong(13, item.memoVersion());
                statement.setTimestamp(14, Timestamp.from(item.statusSavedAt()));
                statement.setTimestamp(15, Timestamp.from(item.statusSavedAt()));
            }

            @Override
            public int getBatchSize() {
                return items.size();
            }
        });
    }
}
