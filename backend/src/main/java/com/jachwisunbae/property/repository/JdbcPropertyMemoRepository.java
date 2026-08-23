package com.jachwisunbae.property.repository;

import com.jachwisunbae.property.entity.PropertyMemo;
import com.jachwisunbae.property.entity.PropertyMemoItem;
import com.jachwisunbae.property.repository.query.PropertyMemoQuery;
import com.jachwisunbae.property.repository.query.PropertyMemoItemQuery;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPropertyMemoRepository implements PropertyMemoRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<PropertyMemoItemQuery> rowMapper = (rs, rowNum) -> new PropertyMemoItemQuery(
            rs.getObject("property_memo_item_id", Long.class),
            rs.getObject("system_memo_item_id", Long.class),
            rs.getString("label"), rs.getObject("display_order", Integer.class), rs.getString("content"));

    public JdbcPropertyMemoRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PropertyMemoQuery findQuery(final long propertyId) {
        String sql = """
                SELECT pm.property_id, pm.free_memo,
                       pmi.id AS property_memo_item_id,
                       pmi.system_memo_item_id,
                       pmi.label, pmi.display_order, pmi.content
                FROM property_memos pm
                JOIN property_memo_items pmi ON pmi.property_memo_id = pm.id
                WHERE pm.property_id = ?
                ORDER BY pmi.display_order, pmi.id
                """;
        List<PropertyMemoItemQuery> items = jdbcTemplate.query(sql, rowMapper, propertyId);
        String freeMemo = items.isEmpty() ? "" : jdbcTemplate.queryForObject(
                "SELECT free_memo FROM property_memos WHERE property_id = ?", String.class, propertyId);
        return new PropertyMemoQuery(propertyId, freeMemo == null ? "" : freeMemo, items);
    }

    @Override
    public Optional<PropertyMemo> findByPropertyId(final long propertyId) {
        return jdbcTemplate.query("SELECT id, property_id, free_memo FROM property_memos WHERE property_id = ?",
                (rs, rowNum) -> PropertyMemo.reconstruct(rs.getLong("id"), rs.getLong("property_id"),
                        rs.getString("free_memo")), propertyId).stream().findFirst();
    }

    @Override
    public PropertyMemo save(final PropertyMemo memo) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO property_memos (property_id, free_memo) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, memo.getPropertyId());
            statement.setString(2, memo.getFreeMemo());
            return statement;
        }, keyHolder);
        return PropertyMemo.reconstruct(keyHolder.getKey().longValue(), memo.getPropertyId(), memo.getFreeMemo());
    }

    @Override
    public void update(final PropertyMemo memo) {
        jdbcTemplate.update("UPDATE property_memos SET free_memo = ? WHERE id = ?",
                memo.getFreeMemo(), memo.getId());
    }

    @Override
    public void deleteByPropertyId(final long propertyId) {
        jdbcTemplate.update("DELETE FROM property_memo_items WHERE property_memo_id IN "
                + "(SELECT id FROM property_memos WHERE property_id = ?)", propertyId);
        jdbcTemplate.update("DELETE FROM property_memos WHERE property_id = ?", propertyId);
    }

    @Override
    public int updateItem(final long propertyMemoId, final long systemMemoItemId, final String content) {
        return jdbcTemplate.update(
                "UPDATE property_memo_items SET content = ? WHERE system_memo_item_id = ? AND property_memo_id = ?",
                content, systemMemoItemId, propertyMemoId);
    }

    @Override
    public void saveItem(final PropertyMemoItem item) {
        jdbcTemplate.update("INSERT INTO property_memo_items "
                        + "(property_memo_id, system_memo_item_id, label, display_order, content) VALUES (?, ?, ?, ?, ?)",
                item.getPropertyMemoId(), item.getSystemMemoItemId(), item.getLabel(), item.getDisplayOrder(),
                item.getContent());
    }

}
