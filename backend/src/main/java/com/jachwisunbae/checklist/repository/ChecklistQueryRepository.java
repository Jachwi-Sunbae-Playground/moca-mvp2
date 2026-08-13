package com.jachwisunbae.checklist.repository;

import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.checklist.service.dto.result.ChecklistDetailResult;
import com.jachwisunbae.checklist.service.dto.result.ChecklistDetailItemResult;
import com.jachwisunbae.checklist.service.dto.result.ChecklistSummaryResult;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.exception.server.DataInconsistencyException;
import com.jachwisunbae.common.page.PageQuery;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ChecklistQueryRepository {

    private static final String FIND_ALL_OWNED_SQL = """
            SELECT checklist.id,
                   checklist.name,
                   checklist.stage,
                   (SELECT COUNT(*)
                    FROM checklist_items item
                    WHERE item.checklist_id = checklist.id) AS item_count,
                   (SELECT COUNT(*)
                    FROM property_active_checklists active
                    WHERE active.checklist_id = checklist.id) AS assigned_property_count,
                   checklist.updated_at
            FROM checklists checklist
            WHERE checklist.member_id = ?
              AND checklist.stage = ?
            ORDER BY checklist.updated_at DESC, checklist.id DESC
            LIMIT ? OFFSET ?
            """;
    private static final String COUNT_ALL_OWNED_SQL = """
            SELECT COUNT(*)
            FROM checklists
            WHERE member_id = ?
              AND stage = ?
            """;
    private static final String FIND_OWNED_DETAIL_SQL = """
            SELECT checklist.id,
                   checklist.name,
                   checklist.stage,
                   checklist.created_at,
                   checklist.updated_at,
                   (SELECT COUNT(*)
                    FROM property_active_checklists active
                    WHERE active.checklist_id = checklist.id) AS assigned_property_count,
                   item.id AS checklist_item_id,
                   item.origin,
                   item.check_item_id,
                   item.item_order,
                   CASE
                       WHEN item.origin = 'PROVIDED' THEN catalog.question
                       ELSE item.custom_question
                   END AS question,
                   CASE
                       WHEN item.origin = 'PROVIDED' THEN catalog.guide
                       ELSE NULL
                   END AS guide
            FROM checklists checklist
            JOIN checklist_items item ON item.checklist_id = checklist.id
            LEFT JOIN check_items catalog ON catalog.id = item.check_item_id
                                         AND catalog.stage = item.stage
            WHERE checklist.id = ?
              AND checklist.member_id = ?
            ORDER BY item.item_order
            """;

    private final JdbcTemplate jdbcTemplate;

    public ChecklistQueryRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ChecklistSummaryResult> findAllOwned(
            final long memberId,
            final CheckStage stage,
            final PageQuery pageQuery
    ) {
        try {
            return jdbcTemplate.query(
                    FIND_ALL_OWNED_SQL,
                    (resultSet, rowNumber) -> new ChecklistSummaryResult(
                            resultSet.getLong("id"),
                            resultSet.getString("name"),
                            CheckStage.valueOf(resultSet.getString("stage")),
                            resultSet.getInt("item_count"),
                            resultSet.getInt("assigned_property_count"),
                            resultSet.getTimestamp("updated_at").toInstant()
                    ),
                    memberId,
                    stage.name(),
                    pageQuery.size(),
                    pageQuery.offset()
            );
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public long countAllOwned(final long memberId, final CheckStage stage) {
        try {
            final Long count = jdbcTemplate.queryForObject(
                    COUNT_ALL_OWNED_SQL,
                    Long.class,
                    memberId,
                    stage.name()
            );
            return count == null ? 0L : count;
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public Optional<ChecklistDetailResult> findOwnedDetail(final long memberId, final long checklistId) {
        try {
            final List<DetailRow> rows = jdbcTemplate.query(
                    FIND_OWNED_DETAIL_SQL,
                    (resultSet, rowNumber) -> new DetailRow(
                            resultSet.getLong("id"),
                            resultSet.getString("name"),
                            CheckStage.valueOf(resultSet.getString("stage")),
                            resultSet.getTimestamp("created_at"),
                            resultSet.getTimestamp("updated_at"),
                            resultSet.getInt("assigned_property_count"),
                            new ChecklistDetailItemResult(
                                    resultSet.getLong("checklist_item_id"),
                                    com.jachwisunbae.checklist.domain.ChecklistItemOrigin.valueOf(
                                            resultSet.getString("origin")
                                    ),
                                    resultSet.getObject("check_item_id", Long.class),
                                    resultSet.getString("question"),
                                    resultSet.getString("guide"),
                                    resultSet.getInt("item_order")
                            )
                    ),
                    checklistId,
                    memberId
            );
            if (rows.isEmpty()) {
                return Optional.empty();
            }
            final DetailRow first = rows.getFirst();
            final List<ChecklistDetailItemResult> items = new ArrayList<>(rows.size());
            rows.forEach(row -> items.add(row.item()));
            return Optional.of(new ChecklistDetailResult(
                    first.checklistId(),
                    first.name(),
                    first.stage(),
                    items,
                    items.size(),
                    first.assignedPropertyCount(),
                    first.createdAt().toInstant(),
                    first.updatedAt().toInstant()
            ));
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    private DataInconsistencyException dataInconsistency(final DataAccessException cause) {
        return new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR, cause);
    }

    private record DetailRow(
            long checklistId,
            String name,
            CheckStage stage,
            Timestamp createdAt,
            Timestamp updatedAt,
            int assignedPropertyCount,
            ChecklistDetailItemResult item
    ) {
    }
}
