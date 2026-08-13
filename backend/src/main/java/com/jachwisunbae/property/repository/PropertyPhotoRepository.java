package com.jachwisunbae.property.repository;

import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.exception.server.DataInconsistencyException;
import com.jachwisunbae.property.domain.PropertyPhoto;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PropertyPhotoRepository {

    private static final String PHOTO_COLUMNS = """
            id,
            property_id,
            member_id,
            storage_key,
            content_type,
            size_bytes,
            checksum_sha256,
            created_at
            """;
    private static final String INSERT_SQL = """
            INSERT INTO property_photos (
                property_id,
                member_id,
                storage_key,
                content_type,
                size_bytes,
                checksum_sha256,
                created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String FIND_ALL_OWNED_SQL = """
            SELECT %s
            FROM property_photos
            WHERE property_id = ?
              AND member_id = ?
            ORDER BY created_at, id
            """.formatted(PHOTO_COLUMNS);
    private static final String FIND_PREVIEW_OWNED_SQL = """
            SELECT %s
            FROM property_photos
            WHERE property_id = ?
              AND member_id = ?
            ORDER BY created_at, id
            LIMIT 1
            """.formatted(PHOTO_COLUMNS);
    private static final String FIND_OWNED_SQL = """
            SELECT %s
            FROM property_photos
            WHERE id = ?
              AND property_id = ?
              AND member_id = ?
            """.formatted(PHOTO_COLUMNS);
    private static final String FIND_STORAGE_KEYS_OWNED_SQL = """
            SELECT storage_key
            FROM property_photos
            WHERE property_id = ?
              AND member_id = ?
            ORDER BY created_at, id
            """;
    private static final String COUNT_OWNED_SQL = """
            SELECT COUNT(*)
            FROM property_photos
            WHERE property_id = ?
              AND member_id = ?
            """;
    private static final String DELETE_OWNED_SQL = """
            DELETE FROM property_photos
            WHERE id = ?
              AND property_id = ?
              AND member_id = ?
            """;
    private static final RowMapper<PropertyPhoto> PHOTO_ROW_MAPPER = (resultSet, rowNumber) -> new PropertyPhoto(
            resultSet.getLong("id"),
            resultSet.getLong("property_id"),
            resultSet.getLong("member_id"),
            resultSet.getString("storage_key"),
            resultSet.getString("content_type"),
            resultSet.getLong("size_bytes"),
            resultSet.getString("checksum_sha256"),
            resultSet.getTimestamp("created_at").toInstant()
    );

    private final JdbcTemplate jdbcTemplate;

    public PropertyPhotoRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PropertyPhoto save(final PropertyPhoto photo) {
        try {
            final KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                final PreparedStatement statement = connection.prepareStatement(
                        INSERT_SQL,
                        Statement.RETURN_GENERATED_KEYS
                );
                statement.setLong(1, photo.propertyId());
                statement.setLong(2, photo.memberId());
                statement.setString(3, photo.storageKey());
                statement.setString(4, photo.contentType());
                statement.setLong(5, photo.sizeBytes());
                statement.setString(6, photo.checksumSha256());
                statement.setTimestamp(7, Timestamp.from(photo.createdAt()));
                return statement;
            }, keyHolder);
            final Number generatedId = keyHolder.getKey();
            if (generatedId == null) {
                throw new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
            return photo.withId(generatedId.longValue());
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public List<PropertyPhoto> findAllOwned(final long memberId, final long propertyId) {
        try {
            return jdbcTemplate.query(FIND_ALL_OWNED_SQL, PHOTO_ROW_MAPPER, propertyId, memberId);
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public List<PropertyPhoto> findPreviewOwned(final long memberId, final long propertyId) {
        try {
            return jdbcTemplate.query(FIND_PREVIEW_OWNED_SQL, PHOTO_ROW_MAPPER, propertyId, memberId);
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public Optional<PropertyPhoto> findOwned(
            final long memberId,
            final long propertyId,
            final long photoId
    ) {
        try {
            return jdbcTemplate.query(FIND_OWNED_SQL, PHOTO_ROW_MAPPER, photoId, propertyId, memberId)
                    .stream()
                    .findFirst();
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public List<String> findStorageKeysOwned(final long memberId, final long propertyId) {
        try {
            return jdbcTemplate.queryForList(FIND_STORAGE_KEYS_OWNED_SQL, String.class, propertyId, memberId);
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public long countOwned(final long memberId, final long propertyId) {
        try {
            final Long count = jdbcTemplate.queryForObject(COUNT_OWNED_SQL, Long.class, propertyId, memberId);
            return count == null ? 0 : count;
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public boolean deleteOwned(final long memberId, final long propertyId, final long photoId) {
        try {
            return jdbcTemplate.update(DELETE_OWNED_SQL, photoId, propertyId, memberId) == 1;
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    private DataInconsistencyException dataInconsistency(final DataAccessException cause) {
        return new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR, cause);
    }
}
