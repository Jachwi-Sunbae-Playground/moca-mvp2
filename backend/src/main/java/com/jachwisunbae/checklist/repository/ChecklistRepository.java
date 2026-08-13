package com.jachwisunbae.checklist.repository;

import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.checklist.domain.Checklist;
import com.jachwisunbae.checklist.domain.ChecklistItem;
import com.jachwisunbae.checklist.domain.ChecklistItemOrigin;
import com.jachwisunbae.checklist.domain.ChecklistName;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.exception.server.DataInconsistencyException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ChecklistRepository {

    private static final String INSERT_SQL = """
            INSERT INTO checklists (member_id, name, stage, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?)
            """;
    private static final String FIND_OWNED_FOR_UPDATE_SQL = """
            SELECT id, member_id, name, stage, created_at, updated_at
            FROM checklists
            WHERE id = ?
              AND member_id = ?
            FOR UPDATE
            """;
    private static final String FIND_ITEM_IDS_SQL = """
            SELECT check_item_id
            FROM checklist_items
            WHERE checklist_id = ?
              AND origin = 'PROVIDED'
            ORDER BY item_order
            """;
    private static final String FIND_ITEMS_FOR_UPDATE_SQL = """
            SELECT id, origin, check_item_id, custom_question, stage, item_order
            FROM checklist_items
            WHERE checklist_id = ?
            ORDER BY item_order
            FOR UPDATE
            """;
    private static final String COUNT_ITEMS_SQL = """
            SELECT COUNT(*)
            FROM checklist_items
            WHERE checklist_id = ?
            """;
    private static final String UPDATE_ROOT_SQL = """
            UPDATE checklists
            SET name = ?, updated_at = ?
            WHERE id = ?
              AND member_id = ?
            """;
    private static final String INSERT_ITEM_SQL = """
            INSERT INTO checklist_items (
                checklist_id, origin, check_item_id, custom_question, stage, item_order
            ) VALUES (?, ?, ?, ?, ?, ?)
            """;
    private static final String MOVE_RETAINED_ITEMS_TO_TEMPORARY_ORDER_SQL = """
            UPDATE checklist_items
            SET item_order = item_order + ?
            WHERE checklist_id = ?
            ORDER BY item_order DESC
            """;
    private static final String UPDATE_RETAINED_ITEM_SQL = """
            UPDATE checklist_items
            SET custom_question = ?,
                item_order = ?,
                updated_at = CURRENT_TIMESTAMP(6)
            WHERE id = ?
              AND checklist_id = ?
              AND origin = ?
            """;
    private static final String DELETE_OWNED_SQL = """
            DELETE FROM checklists
            WHERE id = ?
              AND member_id = ?
            """;
    private static final String FIND_ACTIVE_OWNED_FOR_VISIT_FOR_UPDATE_SQL = """
            SELECT c.id, c.member_id, c.name, c.stage, c.created_at, c.updated_at
            FROM property_active_checklists pac
            JOIN checklists c ON c.id = pac.checklist_id
                             AND c.member_id = pac.member_id
                             AND c.stage = pac.stage
            WHERE pac.property_id = ?
              AND pac.member_id = ?
            ORDER BY c.id
            FOR UPDATE
            """;

    private final JdbcTemplate jdbcTemplate;

    public ChecklistRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Checklist save(final Checklist checklist) {
        try {
            final KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                final PreparedStatement statement = connection.prepareStatement(
                        INSERT_SQL,
                        Statement.RETURN_GENERATED_KEYS
                );
                statement.setLong(1, checklist.memberId());
                statement.setString(2, checklist.name().value());
                statement.setString(3, checklist.stage().name());
                statement.setTimestamp(4, Timestamp.from(checklist.createdAt()));
                statement.setTimestamp(5, Timestamp.from(checklist.updatedAt()));
                return statement;
            }, keyHolder);
            final Number generatedId = keyHolder.getKey();
            if (generatedId == null) {
                throw dataInconsistency();
            }
            final Checklist saved = checklist.withId(generatedId.longValue());
            insertItems(saved.id(), saved.items());
            return saved;
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public Optional<ChecklistRootProjection> findOwnedForUpdate(final long memberId, final long checklistId) {
        try {
            return jdbcTemplate.query(
                    FIND_OWNED_FOR_UPDATE_SQL,
                    (resultSet, rowNumber) -> new ChecklistRootProjection(
                            resultSet.getLong("id"),
                            resultSet.getLong("member_id"),
                            new ChecklistName(resultSet.getString("name")),
                            CheckStage.valueOf(resultSet.getString("stage")),
                            resultSet.getTimestamp("created_at").toInstant(),
                            resultSet.getTimestamp("updated_at").toInstant()
                    ),
                    checklistId,
                    memberId
            ).stream().findFirst();
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public List<Long> findItemIds(final long checklistId) {
        try {
            return jdbcTemplate.queryForList(FIND_ITEM_IDS_SQL, Long.class, checklistId);
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public List<ChecklistItem> findItemsForUpdate(final long checklistId) {
        try {
            return jdbcTemplate.query(
                    FIND_ITEMS_FOR_UPDATE_SQL,
                    (resultSet, rowNumber) -> {
                        final long id = resultSet.getLong("id");
                        final ChecklistItemOrigin origin = ChecklistItemOrigin.valueOf(resultSet.getString("origin"));
                        final CheckStage stage = CheckStage.valueOf(resultSet.getString("stage"));
                        final int order = resultSet.getInt("item_order");
                        if (origin == ChecklistItemOrigin.PROVIDED) {
                            return ChecklistItem.provided(
                                    id,
                                    resultSet.getLong("check_item_id"),
                                    stage,
                                    order
                            );
                        }
                        return ChecklistItem.custom(
                                id,
                                resultSet.getString("custom_question"),
                                stage,
                                order
                        );
                    },
                    checklistId
            );
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public int countItems(final long checklistId) {
        try {
            final Integer count = jdbcTemplate.queryForObject(COUNT_ITEMS_SQL, Integer.class, checklistId);
            return count == null ? 0 : count;
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public boolean updateRoot(final Checklist checklist) {
        try {
            return jdbcTemplate.update(
                    UPDATE_ROOT_SQL,
                    checklist.name().value(),
                    Timestamp.from(checklist.updatedAt()),
                    checklist.id(),
                    checklist.memberId()
            ) == 1;
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public void replaceItems(
            final long checklistId,
            final List<ChecklistItem> existingItems,
            final List<ChecklistItem> items
    ) {
        try {
            deleteRemovedItems(checklistId, items);
            final int temporaryOrderOffset = Math.max(existingItems.size(), items.size());
            if (items.stream().anyMatch(item -> item.id() > 0)) {
                jdbcTemplate.update(
                        MOVE_RETAINED_ITEMS_TO_TEMPORARY_ORDER_SQL,
                        temporaryOrderOffset,
                        checklistId
                );
            }
            for (final ChecklistItem item : items) {
                if (item.id() == 0) {
                    insertItem(checklistId, item);
                } else {
                    updateRetainedItem(checklistId, item);
                }
            }
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public boolean deleteOwned(final long memberId, final long checklistId) {
        try {
            return jdbcTemplate.update(DELETE_OWNED_SQL, checklistId, memberId) == 1;
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public List<ChecklistRootProjection> findActiveOwnedForVisitForUpdate(
            final long memberId,
            final long propertyId
    ) {
        try {
            return jdbcTemplate.query(
                    FIND_ACTIVE_OWNED_FOR_VISIT_FOR_UPDATE_SQL,
                    (resultSet, rowNumber) -> new ChecklistRootProjection(
                            resultSet.getLong("id"),
                            resultSet.getLong("member_id"),
                            new ChecklistName(resultSet.getString("name")),
                            CheckStage.valueOf(resultSet.getString("stage")),
                            resultSet.getTimestamp("created_at").toInstant(),
                            resultSet.getTimestamp("updated_at").toInstant()
                    ),
                    propertyId,
                    memberId
            );
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public List<ChecklistSnapshotItemProjection> findSnapshotItems(final List<Long> checklistIds) {
        if (checklistIds.isEmpty()) {
            return List.of();
        }
        final String placeholders = String.join(", ", java.util.Collections.nCopies(checklistIds.size(), "?"));
        final String sql = """
                SELECT ci.checklist_id,
                       ci.id AS checklist_item_id,
                       ci.origin,
                       ci.check_item_id,
                       CASE
                           WHEN ci.origin = 'PROVIDED' THEN item.question
                           ELSE ci.custom_question
                       END AS question,
                       CASE
                           WHEN ci.origin = 'PROVIDED' THEN item.guide
                           ELSE NULL
                       END AS guide,
                       ci.item_order
                FROM checklist_items ci
                LEFT JOIN check_items item ON item.id = ci.check_item_id
                                          AND item.stage = ci.stage
                WHERE ci.checklist_id IN (%s)
                ORDER BY ci.checklist_id, ci.item_order
                """.formatted(placeholders);
        try {
            return jdbcTemplate.query(
                    sql,
                    (resultSet, rowNumber) -> new ChecklistSnapshotItemProjection(
                            resultSet.getLong("checklist_id"),
                            resultSet.getLong("checklist_item_id"),
                            ChecklistItemOrigin.valueOf(resultSet.getString("origin")),
                            resultSet.getObject("check_item_id", Long.class),
                            resultSet.getString("question"),
                            resultSet.getString("guide"),
                            resultSet.getInt("item_order")
                    ),
                    checklistIds.toArray()
            );
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    private void insertItems(final long checklistId, final List<ChecklistItem> items) {
        jdbcTemplate.batchUpdate(INSERT_ITEM_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(final PreparedStatement statement, final int index) throws java.sql.SQLException {
                final ChecklistItem item = items.get(index);
                statement.setLong(1, checklistId);
                statement.setString(2, item.origin().name());
                if (item.sourceCheckItemId() == null) {
                    statement.setObject(3, null);
                } else {
                    statement.setLong(3, item.sourceCheckItemId());
                }
                statement.setString(4, item.customQuestion());
                statement.setString(5, item.stage().name());
                statement.setInt(6, item.order());
            }

            @Override
            public int getBatchSize() {
                return items.size();
            }
        });
    }

    private void deleteRemovedItems(final long checklistId, final List<ChecklistItem> items) {
        final List<Long> retainedIds = items.stream()
                .map(ChecklistItem::id)
                .filter(id -> id > 0)
                .toList();
        if (retainedIds.isEmpty()) {
            jdbcTemplate.update("DELETE FROM checklist_items WHERE checklist_id = ?", checklistId);
            return;
        }
        final String placeholders = String.join(", ", java.util.Collections.nCopies(retainedIds.size(), "?"));
        final String sql = "DELETE FROM checklist_items WHERE checklist_id = ? AND id NOT IN (%s)"
                .formatted(placeholders);
        final Object[] arguments = new Object[retainedIds.size() + 1];
        arguments[0] = checklistId;
        for (int index = 0; index < retainedIds.size(); index++) {
            arguments[index + 1] = retainedIds.get(index);
        }
        jdbcTemplate.update(sql, arguments);
    }

    private void insertItem(final long checklistId, final ChecklistItem item) {
        jdbcTemplate.update(
                INSERT_ITEM_SQL,
                checklistId,
                item.origin().name(),
                item.sourceCheckItemId(),
                item.customQuestion(),
                item.stage().name(),
                item.order()
        );
    }

    private void updateRetainedItem(final long checklistId, final ChecklistItem item) {
        final int updated = jdbcTemplate.update(
                UPDATE_RETAINED_ITEM_SQL,
                item.customQuestion(),
                item.order(),
                item.id(),
                checklistId,
                item.origin().name()
        );
        if (updated != 1) {
            throw dataInconsistency();
        }
    }

    private DataInconsistencyException dataInconsistency() {
        return new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private DataInconsistencyException dataInconsistency(final DataAccessException cause) {
        return new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR, cause);
    }
}
