package com.jachwisunbae.property.repository;

import com.jachwisunbae.checklist.type.CheckStage;
import com.jachwisunbae.checklist.type.CheckStatus;
import com.jachwisunbae.property.repository.query.PropertyChecklistApplicationQuery;
import com.jachwisunbae.property.repository.query.PropertyChecklistItemQuery;
import com.jachwisunbae.property.repository.query.PropertyChecklistItemStateQuery;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
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
    public List<PropertyChecklistItemStateQuery> findCurrentItems(final long propertyId, final CheckStage stage) {
        return jdbcTemplate.query("SELECT pci.system_check_item_id, pci.question, pci.display_order, pci.status, pci.memo "
                        + "FROM property_checklists pc JOIN property_checklist_items pci "
                        + "ON pci.property_checklist_id = pc.id WHERE pc.property_id = ? AND pc.stage = ?",
                (rs, rowNum) -> new PropertyChecklistItemStateQuery(rs.getLong("system_check_item_id"),
                        rs.getString("question"), rs.getInt("display_order"),
                        CheckStatus.valueOf(rs.getString("status")), rs.getString("memo")),
                propertyId, stage.name());
    }

    @Override
    public void deleteByPropertyAndStage(final long propertyId, final CheckStage stage) {
        jdbcTemplate.update("DELETE FROM property_checklist_items WHERE property_checklist_id IN "
                + "(SELECT id FROM property_checklists WHERE property_id = ? AND stage = ?)", propertyId, stage.name());
        jdbcTemplate.update("DELETE FROM property_checklists WHERE property_id = ? AND stage = ?", propertyId, stage.name());
    }

    @Override
    public long save(final long propertyId, final long sourceChecklistId, final String checklistName,
                     final CheckStage stage) {
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
        return keyHolder.getKey().longValue();
    }

    @Override
    public void saveItems(final long propertyChecklistId, final List<PropertyChecklistItemStateQuery> items) {
        String sql = "INSERT INTO property_checklist_items "
                + "(property_checklist_id, system_check_item_id, display_order, status, memo, question) VALUES (?, ?, ?, ?, ?, ?)";
        List<Object[]> params = items.stream().map(item -> {
            return new Object[]{propertyChecklistId, item.systemCheckItemId(), item.displayOrder(),
                    item.status().name(), item.memo(), item.question()};
        }).toList();
        jdbcTemplate.batchUpdate(sql, params);
    }

    @Override
    public Optional<PropertyChecklistApplicationQuery> findApplication(final long memberId, final long propertyId,
                                                                        final long propertyChecklistId) {
        Optional<PropertyChecklistApplicationQuery> checklist = jdbcTemplate.query(
                        "SELECT pc.id, pc.property_id, pc.user_checklist_id, pc.checklist_name, pc.stage "
                                + "FROM property_checklists pc "
                                + "JOIN properties p ON p.id = pc.property_id "
                                + "WHERE p.id = ? AND p.member_id = ? AND pc.id = ?",
                        (rs, row) -> new PropertyChecklistApplicationQuery(rs.getLong("id"),
                                rs.getLong("property_id"), rs.getObject("user_checklist_id", Long.class),
                                rs.getString("checklist_name"), CheckStage.valueOf(rs.getString("stage")), List.of()),
                        propertyId, memberId, propertyChecklistId)
                .stream().findFirst();
        if (checklist.isEmpty()) {
            return Optional.empty();
        }
        List<PropertyChecklistItemQuery> savedItems = jdbcTemplate.query(
                "SELECT id, system_check_item_id, question, display_order, status, memo "
                        + "FROM property_checklist_items WHERE property_checklist_id = ? ORDER BY display_order ASC, id ASC",
                (rs, row) -> new PropertyChecklistItemQuery(rs.getLong("id"), rs.getLong("system_check_item_id"),
                        rs.getString("question"), rs.getInt("display_order"),
                        CheckStatus.valueOf(rs.getString("status")), rs.getString("memo")), propertyChecklistId);
        PropertyChecklistApplicationQuery root = checklist.get();
        return Optional.of(new PropertyChecklistApplicationQuery(root.id(), root.propertyId(), root.sourceChecklistId(),
                root.checklistName(), root.stage(), savedItems));
    }

    @Override
    public int updateStatus(final long memberId, final long propertyId, final long propertyChecklistId,
                            final long itemId, final String status) {
        return jdbcTemplate.update("UPDATE property_checklist_items pci "
                        + "JOIN property_checklists pc ON pc.id = pci.property_checklist_id "
                        + "JOIN properties p ON p.id = pc.property_id "
                        + "SET pci.status = ? WHERE p.id = ? AND p.member_id = ? "
                        + "AND pc.id = ? AND pci.id = ?",
                status, propertyId, memberId, propertyChecklistId, itemId);
    }

    @Override
    public int updateMemo(final long memberId, final long propertyId, final long propertyChecklistId,
                          final long itemId, final String memo) {
        return jdbcTemplate.update("UPDATE property_checklist_items pci "
                        + "JOIN property_checklists pc ON pc.id = pci.property_checklist_id "
                        + "JOIN properties p ON p.id = pc.property_id "
                        + "SET pci.memo = ? WHERE p.id = ? AND p.member_id = ? "
                        + "AND pc.id = ? AND pci.id = ?",
                memo, propertyId, memberId, propertyChecklistId, itemId);
    }

    @Override
    public Optional<PropertyChecklistItemQuery> findItem(final long memberId, final long propertyId,
                                                         final long propertyChecklistId, final long itemId) {
        return jdbcTemplate.query("SELECT pci.id, pci.system_check_item_id, pci.question, pci.display_order, "
                        + "pci.status, pci.memo FROM property_checklist_items pci "
                        + "JOIN property_checklists pc ON pc.id = pci.property_checklist_id "
                        + "JOIN properties p ON p.id = pc.property_id "
                        + "WHERE p.id = ? AND p.member_id = ? AND pc.id = ? AND pci.id = ?",
                (rs, row) -> new PropertyChecklistItemQuery(rs.getLong("id"),
                        rs.getLong("system_check_item_id"), rs.getString("question"),
                        rs.getInt("display_order"), CheckStatus.valueOf(rs.getString("status")),
                        rs.getString("memo")), propertyId, memberId, propertyChecklistId, itemId)
                .stream().findFirst();
    }
}
