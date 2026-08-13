package com.jachwisunbae.property.repository;

import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.exception.server.DataInconsistencyException;
import com.jachwisunbae.property.domain.DiscoverySource;
import com.jachwisunbae.property.domain.DiscoverySourceType;
import com.jachwisunbae.property.domain.Money;
import com.jachwisunbae.property.domain.Property;
import com.jachwisunbae.property.domain.PropertyMemo;
import com.jachwisunbae.property.domain.PropertyName;
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
public class PropertyRepository {

    private static final String INSERT_SQL = """
            INSERT INTO properties (
                member_id,
                name,
                deposit_amount,
                monthly_rent_amount,
                discovery_source_type,
                discovery_source,
                memo,
                memo_updated_at,
                last_activity_at,
                created_at,
                updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String FIND_OWNED_FOR_UPDATE_SQL = """
            SELECT id,
                   member_id,
                   name,
                   deposit_amount,
                   monthly_rent_amount,
                   discovery_source_type,
                   discovery_source,
                   memo,
                   memo_updated_at,
                   last_activity_at,
                   created_at,
                   updated_at
            FROM properties
            WHERE id = ?
              AND member_id = ?
            FOR UPDATE
            """;
    private static final String UPDATE_BASIC_INFO_SQL = """
            UPDATE properties
            SET name = ?,
                deposit_amount = ?,
                monthly_rent_amount = ?,
                discovery_source_type = ?,
                discovery_source = ?,
                last_activity_at = ?,
                updated_at = ?
            WHERE id = ?
              AND member_id = ?
            """;
    private static final String UPDATE_MEMO_SQL = """
            UPDATE properties
            SET memo = ?,
                memo_updated_at = ?,
                last_activity_at = ?,
                updated_at = ?
            WHERE id = ?
              AND member_id = ?
            """;
    private static final String DELETE_OWNED_SQL = """
            DELETE FROM properties
            WHERE id = ?
              AND member_id = ?
            """;
    private static final String EXISTS_OWNED_SQL = """
            SELECT EXISTS(
                SELECT 1
                FROM properties
                WHERE id = ?
                  AND member_id = ?
            )
            """;
    private static final String UPDATE_LAST_ACTIVITY_SQL = """
            UPDATE properties
            SET last_activity_at = ?
            WHERE id = ?
              AND member_id = ?
            """;
    private static final RowMapper<Property> PROPERTY_ROW_MAPPER = (resultSet, rowNumber) -> {
        final Timestamp memoUpdatedAt = resultSet.getTimestamp("memo_updated_at");
        return new Property(
                resultSet.getLong("id"),
                resultSet.getLong("member_id"),
                new PropertyName(resultSet.getString("name")),
                new Money(resultSet.getLong("deposit_amount")),
                new Money(resultSet.getLong("monthly_rent_amount")),
                new DiscoverySource(
                        DiscoverySourceType.valueOf(resultSet.getString("discovery_source_type")),
                        resultSet.getString("discovery_source")
                ),
                new PropertyMemo(resultSet.getString("memo")),
                memoUpdatedAt == null ? null : memoUpdatedAt.toInstant(),
                resultSet.getTimestamp("last_activity_at").toInstant(),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant()
        );
    };

    private final JdbcTemplate jdbcTemplate;

    public PropertyRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Property save(final Property property) {
        try {
            final KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                final PreparedStatement statement = connection.prepareStatement(
                        INSERT_SQL,
                        Statement.RETURN_GENERATED_KEYS
                );
                statement.setLong(1, property.memberId());
                statement.setString(2, property.name().value());
                statement.setLong(3, property.depositAmount().amount());
                statement.setLong(4, property.monthlyRentAmount().amount());
                statement.setString(5, property.discoverySource().type().name());
                statement.setString(6, property.discoverySource().value());
                statement.setString(7, property.memo().content());
                statement.setTimestamp(8, null);
                statement.setTimestamp(9, Timestamp.from(property.lastActivityAt()));
                statement.setTimestamp(10, Timestamp.from(property.createdAt()));
                statement.setTimestamp(11, Timestamp.from(property.updatedAt()));
                return statement;
            }, keyHolder);
            final Number generatedId = keyHolder.getKey();
            if (generatedId == null) {
                throw new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
            return property.withId(generatedId.longValue());
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public Optional<Property> findOwnedByIdForUpdate(final long memberId, final long propertyId) {
        try {
            return jdbcTemplate.query(
                    FIND_OWNED_FOR_UPDATE_SQL,
                    PROPERTY_ROW_MAPPER,
                    propertyId,
                    memberId
            ).stream().findFirst();
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public boolean updateBasicInfo(final Property property) {
        try {
            return jdbcTemplate.update(
                    UPDATE_BASIC_INFO_SQL,
                    property.name().value(),
                    property.depositAmount().amount(),
                    property.monthlyRentAmount().amount(),
                    property.discoverySource().type().name(),
                    property.discoverySource().value(),
                    Timestamp.from(property.lastActivityAt()),
                    Timestamp.from(property.updatedAt()),
                    property.id(),
                    property.memberId()
            ) == 1;
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public boolean updateMemo(final Property property) {
        try {
            final Timestamp savedAt = Timestamp.from(property.memoUpdatedAt());
            return jdbcTemplate.update(
                    UPDATE_MEMO_SQL,
                    property.memo().content(),
                    savedAt,
                    Timestamp.from(property.lastActivityAt()),
                    Timestamp.from(property.updatedAt()),
                    property.id(),
                    property.memberId()
            ) == 1;
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public boolean deleteOwned(final long memberId, final long propertyId) {
        try {
            return jdbcTemplate.update(DELETE_OWNED_SQL, propertyId, memberId) == 1;
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public boolean existsOwned(final long memberId, final long propertyId) {
        try {
            final Boolean exists = jdbcTemplate.queryForObject(
                    EXISTS_OWNED_SQL,
                    Boolean.class,
                    propertyId,
                    memberId
            );
            return Boolean.TRUE.equals(exists);
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public boolean updateLastActivity(
            final long memberId,
            final long propertyId,
            final Instant lastActivityAt
    ) {
        try {
            return jdbcTemplate.update(
                    UPDATE_LAST_ACTIVITY_SQL,
                    Timestamp.from(lastActivityAt),
                    propertyId,
                    memberId
            ) == 1;
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    private DataInconsistencyException dataInconsistency(final DataAccessException cause) {
        return new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR, cause);
    }
}
