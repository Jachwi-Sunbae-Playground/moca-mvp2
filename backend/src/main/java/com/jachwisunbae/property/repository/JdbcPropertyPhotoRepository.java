package com.jachwisunbae.property.repository;

import com.jachwisunbae.property.entity.PropertyPhoto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

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
                ORDER BY created_at ASC, id ASC
                """;
        return jdbcTemplate.query(sql, propertyPhotoRowMapper, propertyId);
    }

    @Override
    public Optional<PropertyPhoto> findByIdAndPropertyId(final long photoId, final long propertyId) {
        return jdbcTemplate.query("""
                SELECT id, property_id, storage_key, content_type, size_bytes, created_at
                FROM property_photos WHERE id = ? AND property_id = ?
                """, propertyPhotoRowMapper, photoId, propertyId).stream().findFirst();
    }

    @Override
    public void deleteById(final long photoId) {
        jdbcTemplate.update("DELETE FROM main_property_photos WHERE property_photos_id = ?", photoId);
        jdbcTemplate.update("DELETE FROM property_photos WHERE id = ?", photoId);
    }

    @Override
    public void deleteByPropertyId(final long propertyId) {
        jdbcTemplate.update("DELETE FROM main_property_photos WHERE property_id = ?", propertyId);
        jdbcTemplate.update("DELETE FROM property_photos WHERE property_id = ?", propertyId);
    }

    @Override
    public void ensureRepresentative(final long propertyId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM main_property_photos WHERE property_id = ?", Integer.class, propertyId);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.query("SELECT id FROM property_photos WHERE property_id = ? "
                        + "ORDER BY created_at ASC, id ASC LIMIT 1",
                (rs, row) -> rs.getLong("id"), propertyId).stream().findFirst()
                .ifPresent(photoId -> jdbcTemplate.update(
                        "INSERT INTO main_property_photos (property_id, property_photos_id) VALUES (?, ?)",
                        propertyId, photoId));
    }

    @Override
    public Optional<Long> findRepresentativePhotoId(final long propertyId) {
        return jdbcTemplate.query("SELECT property_photos_id FROM main_property_photos WHERE property_id = ?",
                (rs, row) -> rs.getLong("property_photos_id"), propertyId).stream().findFirst();
    }

    @Override
    public void setRepresentative(final long propertyId, final long photoId) {
        jdbcTemplate.update("DELETE FROM main_property_photos WHERE property_id = ?", propertyId);
        jdbcTemplate.update("INSERT INTO main_property_photos (property_id, property_photos_id) VALUES (?, ?)",
                propertyId, photoId);
    }
}
