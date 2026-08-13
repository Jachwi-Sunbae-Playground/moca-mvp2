package com.jachwisunbae.visit.repository;

import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.exception.server.DataInconsistencyException;
import com.jachwisunbae.visit.domain.Visit;
import com.jachwisunbae.visit.domain.VisitStatus;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class VisitRepository {

    private static final String INSERT_SQL = """
            INSERT INTO visits (property_id, member_id, status, started_at, completed_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
    private static final String FIND_OWNED_PROPERTY_ID_SQL = """
            SELECT property_id
            FROM visits
            WHERE id = ?
              AND member_id = ?
            """;
    private static final String FIND_OWNED_FOR_UPDATE_SQL = """
            SELECT id, property_id, member_id, status, started_at, completed_at, updated_at
            FROM visits
            WHERE id = ?
              AND member_id = ?
            FOR UPDATE
            """;
    private static final String UPDATE_COMPLETION_SQL = """
            UPDATE visits
            SET status = ?, completed_at = ?, updated_at = ?
            WHERE id = ?
              AND member_id = ?
              AND status = 'IN_PROGRESS'
            """;
    private static final String UPDATE_ACTIVITY_SQL = """
            UPDATE visits
            SET updated_at = ?
            WHERE id = ?
              AND member_id = ?
            """;
    private static final String COUNT_COMPLETED_PROPERTIES_SQL = """
            SELECT COUNT(DISTINCT property_id)
            FROM visits
            WHERE member_id = ?
              AND status = 'COMPLETED'
            """;
    private static final RowMapper<Visit> VISIT_ROW_MAPPER = (resultSet, rowNumber) -> {
        final Timestamp completedAt = resultSet.getTimestamp("completed_at");
        return new Visit(
                resultSet.getLong("id"),
                resultSet.getLong("property_id"),
                resultSet.getLong("member_id"),
                VisitStatus.valueOf(resultSet.getString("status")),
                resultSet.getTimestamp("started_at").toInstant(),
                completedAt == null ? null : completedAt.toInstant(),
                resultSet.getTimestamp("updated_at").toInstant()
        );
    };

    private final JdbcTemplate jdbcTemplate;

    public VisitRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Visit save(final Visit visit) {
        try {
            final KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                final PreparedStatement statement = connection.prepareStatement(
                        INSERT_SQL,
                        Statement.RETURN_GENERATED_KEYS
                );
                statement.setLong(1, visit.propertyId());
                statement.setLong(2, visit.memberId());
                statement.setString(3, visit.status().name());
                statement.setTimestamp(4, Timestamp.from(visit.startedAt()));
                statement.setTimestamp(5, null);
                statement.setTimestamp(6, Timestamp.from(visit.updatedAt()));
                return statement;
            }, keyHolder);
            final Number key = keyHolder.getKey();
            if (key == null) {
                throw dataInconsistency();
            }
            return visit.withId(key.longValue());
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public Optional<Long> findOwnedPropertyId(final long memberId, final long visitId) {
        try {
            return jdbcTemplate.query(
                    FIND_OWNED_PROPERTY_ID_SQL,
                    (resultSet, rowNumber) -> resultSet.getLong("property_id"),
                    visitId,
                    memberId
            ).stream().findFirst();
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public Optional<Visit> findOwnedForUpdate(final long memberId, final long visitId) {
        try {
            return jdbcTemplate.query(FIND_OWNED_FOR_UPDATE_SQL, VISIT_ROW_MAPPER, visitId, memberId)
                    .stream()
                    .findFirst();
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public boolean complete(final Visit visit) {
        try {
            return jdbcTemplate.update(
                    UPDATE_COMPLETION_SQL,
                    visit.status().name(),
                    Timestamp.from(visit.completedAt()),
                    Timestamp.from(visit.updatedAt()),
                    visit.id(),
                    visit.memberId()
            ) == 1;
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public boolean updateActivity(final long memberId, final long visitId, final Instant updatedAt) {
        try {
            return jdbcTemplate.update(
                    UPDATE_ACTIVITY_SQL,
                    Timestamp.from(updatedAt),
                    visitId,
                    memberId
            ) == 1;
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public long countCompletedProperties(final long memberId) {
        try {
            final Long count = jdbcTemplate.queryForObject(
                    COUNT_COMPLETED_PROPERTIES_SQL,
                    Long.class,
                    memberId
            );
            return count == null ? 0 : count;
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    private DataInconsistencyException dataInconsistency() {
        return new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private DataInconsistencyException dataInconsistency(final DataAccessException cause) {
        return new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR, cause);
    }
}
