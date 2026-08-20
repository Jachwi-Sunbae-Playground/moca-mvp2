package com.jachwisunbae.property.repository;

import com.jachwisunbae.checklist.entity.UserChecklistItem;
import com.jachwisunbae.checklist.type.CheckStage;
import com.jachwisunbae.checklist.type.CheckStatus;
import com.jachwisunbae.property.repository.query.PropertyChecklistApplicationQuery;
import com.jachwisunbae.property.repository.query.PropertyChecklistItemQuery;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
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

    @Override
    public PropertyChecklistApplicationQuery replace(final long propertyId, final long sourceChecklistId,
                                                     final String checklistName, final CheckStage stage,
                                                     final List<UserChecklistItem> items) {
        Map<Long, String[]> previous = new HashMap<>();
        jdbcTemplate.queryForList("SELECT pci.system_check_item_id, pci.status, pci.memo "
                        + "FROM property_checklists pc JOIN property_checklist_items pci "
                        + "ON pci.property_checklist_id = pc.id WHERE pc.property_id = ? AND pc.stage = ?",
                propertyId, stage.name()).forEach(row -> previous.put(
                        ((Number) row.get("system_check_item_id")).longValue(),
                        new String[]{(String) row.get("status"), (String) row.get("memo")}));
        jdbcTemplate.update("DELETE FROM property_checklist_items WHERE property_checklist_id IN "
                + "(SELECT id FROM property_checklists WHERE property_id = ? AND stage = ?)", propertyId, stage.name());
        jdbcTemplate.update("DELETE FROM property_checklists WHERE property_id = ? AND stage = ?", propertyId, stage.name());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO property_checklists (property_id, user_checklist_id, checklist_name, stage) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, propertyId);
            statement.setLong(2, sourceChecklistId);
            statement.setString(3, checklistName);
            statement.setString(4, stage.name());
            return statement;
        }, keyHolder);
        long propertyChecklistId = keyHolder.getKey().longValue();
        String sql = "INSERT INTO property_checklist_items "
                + "(property_checklist_id, system_check_item_id, display_order, status, memo, question) VALUES (?, ?, ?, ?, ?, ?)";
        List<Object[]> params = items.stream().map(item -> {
            String[] old = previous.get(item.getSystemCheckItemId());
            return new Object[]{propertyChecklistId, item.getSystemCheckItemId(), item.getDisplayOrder(),
                    old == null ? CheckStatus.UNCONFIRMED.name() : old[0], old == null ? "" : old[1], item.getQuestion()};
        }).toList();
        jdbcTemplate.batchUpdate(sql, params);
        List<PropertyChecklistItemQuery> savedItems = jdbcTemplate.query(
                "SELECT id, system_check_item_id, question, display_order, status, memo "
                        + "FROM property_checklist_items WHERE property_checklist_id = ? ORDER BY display_order ASC, id ASC",
                (rs, row) -> new PropertyChecklistItemQuery(rs.getLong("id"), rs.getLong("system_check_item_id"),
                        rs.getString("question"), rs.getInt("display_order"),
                        CheckStatus.valueOf(rs.getString("status")), rs.getString("memo")), propertyChecklistId);
        return new PropertyChecklistApplicationQuery(propertyChecklistId, propertyId, sourceChecklistId,
                checklistName, stage, savedItems);
    }
}
