package com.jachwisunbae.property.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPropertyChecklistRepository implements PropertyChecklistRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcPropertyChecklistRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void deleteByPropertyId(final long propertyId) {
        jdbcTemplate.update("DELETE FROM property_checklist_items WHERE property_checklist_id IN "
                + "(SELECT id FROM property_checklists WHERE property_id = ?)", propertyId);
        jdbcTemplate.update("DELETE FROM property_checklists WHERE property_id = ?", propertyId);
    }
}
