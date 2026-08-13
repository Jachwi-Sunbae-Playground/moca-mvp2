package com.jachwisunbae.property.repository;

import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.exception.server.DataInconsistencyException;
import com.jachwisunbae.property.domain.ActiveChecklist;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ActiveChecklistRepository {

    private static final String FIND_OWNED_SQL = """
            SELECT active.property_id,
                   active.member_id,
                   active.stage,
                   active.checklist_id,
                   active.created_at,
                   active.updated_at
            FROM property_active_checklists active
            JOIN properties property ON property.id = active.property_id
                                    AND property.member_id = active.member_id
            WHERE active.property_id = ?
              AND active.member_id = ?
              AND active.stage = ?
              AND property.member_id = ?
            """;
    private static final String UPSERT_OWNED_SQL = """
            INSERT INTO property_active_checklists (
                property_id, member_id, stage, checklist_id, created_at, updated_at
            )
            SELECT property.id, property.member_id, ?, checklist.id, ?, ?
            FROM properties property
            JOIN checklists checklist ON checklist.id = ?
                                     AND checklist.member_id = property.member_id
                                     AND checklist.stage = ?
            WHERE property.id = ?
              AND property.member_id = ?
            ON DUPLICATE KEY UPDATE
                member_id = VALUES(member_id),
                checklist_id = VALUES(checklist_id),
                updated_at = VALUES(updated_at)
            """;
    private static final String DELETE_OWNED_SQL = """
            DELETE active
            FROM property_active_checklists active
            JOIN properties property ON property.id = active.property_id
                                    AND property.member_id = active.member_id
            WHERE active.property_id = ?
              AND active.member_id = ?
              AND active.stage = ?
              AND property.member_id = ?
            """;
    private static final String FIND_ALL_OWNED_SQL = """
            SELECT active.property_id,
                   active.stage,
                   active.checklist_id,
                   checklist.name,
                   COUNT(item.id) AS item_count
            FROM property_active_checklists active
            JOIN properties property ON property.id = active.property_id
                                    AND property.member_id = active.member_id
            JOIN checklists checklist ON checklist.id = active.checklist_id
                                     AND checklist.member_id = active.member_id
                                     AND checklist.stage = active.stage
            LEFT JOIN checklist_items item ON item.checklist_id = checklist.id
            WHERE active.property_id = ?
              AND active.member_id = ?
              AND property.member_id = ?
            GROUP BY active.property_id,
                     active.stage,
                     active.checklist_id,
                     checklist.name
            ORDER BY FIELD(active.stage, 'ONLINE_PHONE', 'ON_SITE', 'PRE_CONTRACT')
            """;

    private final JdbcTemplate jdbcTemplate;

    public ActiveChecklistRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<ActiveChecklist> findOwned(
            final long memberId,
            final long propertyId,
            final CheckStage stage
    ) {
        try {
            return jdbcTemplate.query(
                    FIND_OWNED_SQL,
                    (resultSet, rowNumber) -> new ActiveChecklist(
                            resultSet.getLong("property_id"),
                            resultSet.getLong("member_id"),
                            CheckStage.valueOf(resultSet.getString("stage")),
                            resultSet.getLong("checklist_id"),
                            resultSet.getTimestamp("created_at").toInstant(),
                            resultSet.getTimestamp("updated_at").toInstant()
                    ),
                    propertyId,
                    memberId,
                    stage.name(),
                    memberId
            ).stream().findFirst();
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public boolean upsertOwned(final ActiveChecklist activeChecklist) {
        try {
            return jdbcTemplate.update(
                    UPSERT_OWNED_SQL,
                    activeChecklist.stage().name(),
                    Timestamp.from(activeChecklist.createdAt()),
                    Timestamp.from(activeChecklist.updatedAt()),
                    activeChecklist.checklistId(),
                    activeChecklist.stage().name(),
                    activeChecklist.propertyId(),
                    activeChecklist.memberId()
            ) > 0;
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public boolean deleteOwned(
            final long memberId,
            final long propertyId,
            final CheckStage stage
    ) {
        try {
            return jdbcTemplate.update(
                    DELETE_OWNED_SQL,
                    propertyId,
                    memberId,
                    stage.name(),
                    memberId
            ) == 1;
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public List<ActiveChecklistProjection> findAllOwned(final long memberId, final long propertyId) {
        try {
            return jdbcTemplate.query(
                    FIND_ALL_OWNED_SQL,
                    (resultSet, rowNumber) -> new ActiveChecklistProjection(
                            resultSet.getLong("property_id"),
                            CheckStage.valueOf(resultSet.getString("stage")),
                            resultSet.getLong("checklist_id"),
                            resultSet.getString("name"),
                            resultSet.getInt("item_count")
                    ),
                    propertyId,
                    memberId,
                    memberId
            );
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    private DataInconsistencyException dataInconsistency(final DataAccessException cause) {
        return new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR, cause);
    }
}
