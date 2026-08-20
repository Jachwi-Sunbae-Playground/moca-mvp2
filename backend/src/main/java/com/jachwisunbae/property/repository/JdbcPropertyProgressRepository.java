package com.jachwisunbae.property.repository;

import com.jachwisunbae.property.repository.query.PropertyProgressSummary;
import com.jachwisunbae.property.repository.query.PropertyChecklistProgressQuery;
import com.jachwisunbae.checklist.type.CheckStage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class JdbcPropertyProgressRepository implements PropertyProgressRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<PropertyProgressSummary> rowMapper = (rs, row) ->
            new PropertyProgressSummary(rs.getLong("property_id"), rs.getInt("total_count"), rs.getInt("completed_count"),
                    rs.getInt("good_count"), rs.getInt("caution_count"), rs.getInt("unconfirmed_count"));

    public JdbcPropertyProgressRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PropertyProgressSummary findByPropertyId(final long propertyId) {
        String sql = """
                SELECT ? AS property_id, COUNT(pci.id) AS total_count,
                       COALESCE(SUM(pci.status IN ('GOOD', 'CAUTION')), 0) AS completed_count,
                       COALESCE(SUM(pci.status = 'GOOD'), 0) AS good_count,
                       COALESCE(SUM(pci.status = 'CAUTION'), 0) AS caution_count,
                       COALESCE(SUM(pci.status = 'UNCONFIRMED'), 0) AS unconfirmed_count
                FROM property_checklists pc
                LEFT JOIN property_checklist_items pci ON pci.property_checklist_id = pc.id
                WHERE pc.property_id = ?
                """;
        return jdbcTemplate.queryForObject(sql, rowMapper, propertyId, propertyId);
    }

    @Override
    public List<PropertyChecklistProgressQuery> findByPropertyIdAndStage(final long propertyId) {
        String sql = """
                SELECT pc.stage, pc.id AS property_checklist_id, pc.checklist_name,
                       pc.user_checklist_id AS source_checklist_id,
                       COUNT(pci.id) AS total_count,
                       COALESCE(SUM(pci.status IN ('GOOD', 'CAUTION')), 0) AS completed_count,
                       COALESCE(SUM(pci.status = 'GOOD'), 0) AS good_count,
                       COALESCE(SUM(pci.status = 'CAUTION'), 0) AS caution_count,
                       COALESCE(SUM(pci.status = 'UNCONFIRMED'), 0) AS unconfirmed_count
                FROM property_checklists pc
                LEFT JOIN property_checklist_items pci ON pci.property_checklist_id = pc.id
                WHERE pc.property_id = ?
                GROUP BY pc.stage, pc.id, pc.checklist_name, pc.user_checklist_id
                """;
        return jdbcTemplate.query(sql, (rs, row) -> new PropertyChecklistProgressQuery(
                CheckStage.valueOf(rs.getString("stage")),
                rs.getLong("property_checklist_id"), rs.getString("checklist_name"),
                rs.getObject("source_checklist_id", Long.class),
                new PropertyProgressSummary(propertyId, rs.getInt("total_count"),
                        rs.getInt("completed_count"), rs.getInt("good_count"),
                        rs.getInt("caution_count"), rs.getInt("unconfirmed_count"))), propertyId);
    }
}
