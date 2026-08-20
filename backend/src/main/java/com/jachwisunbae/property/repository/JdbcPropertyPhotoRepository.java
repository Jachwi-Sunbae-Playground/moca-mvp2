package com.jachwisunbae.property.repository;

import com.jachwisunbae.property.entity.PropertyPhoto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class JdbcPropertyPhotoRepository implements PropertyPhotoRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<PropertyPhoto> propertyPhotoRowMapper = (rs, row) -> PropertyPhoto.reconstruct(
            rs.getLong("id"), rs.getLong("property_id"), rs.getString("storage_key"),
            rs.getString("content_type"), rs.getLong("size_bytes"),
            rs.getTimestamp("created_at").toLocalDateTime());

    public JdbcPropertyPhotoRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<PropertyPhoto> findByPropertyId(final long propertyId) {
        String sql = """
                SELECT id, property_id, storage_key, content_type, size_bytes, created_at
                FROM property_photos
                WHERE property_id = ?
                ORDER BY created_at, id
                """;
        return jdbcTemplate.query(sql, propertyPhotoRowMapper, propertyId);
    }
}
