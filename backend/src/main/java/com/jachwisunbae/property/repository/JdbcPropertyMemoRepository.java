package com.jachwisunbae.property.repository;

import com.jachwisunbae.property.entity.PropertyMemo;
import com.jachwisunbae.property.entity.PropertyMemoItem;
import com.jachwisunbae.property.repository.query.PropertyMemoRow;
import com.jachwisunbae.property.repository.query.PropertyMemoItemRow;
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
    private final RowMapper<PropertyMemoItemRow> rowMapper = (rs, rowNum) -> new PropertyMemoItemRow(
            rs.getObject("property_memo_item_id", Long.class),
            rs.getObject("system_memo_item_id", Long.class),
            rs.getString("label"), rs.getObject("display_order", Integer.class), rs.getString("content"));

    public JdbcPropertyMemoRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PropertyMemoRow findRows(final long propertyId) {
        String sql = """
                SELECT ? AS property_id, pm.id AS property_memo_id, pm.free_memo,
                       pmi.id AS property_memo_item_id,
                       sm.id AS system_memo_item_id,
                       COALESCE(pmi.label, sm.label) AS label,
                       COALESCE(pmi.display_order, sm.display_order) AS display_order,
                       COALESCE(pmi.content, '') AS content
                FROM system_memo_items sm
                LEFT JOIN property_memos pm ON pm.property_id = ?
                LEFT JOIN property_memo_items pmi
                    ON pmi.property_memo_id = pm.id AND pmi.system_meno_id = sm.id
                WHERE sm.deleted_at IS NULL
                ORDER BY sm.display_order, sm.id
                """;
        List<PropertyMemoItemRow> items = jdbcTemplate.query(sql, rowMapper, propertyId, propertyId);
        String freeMemo = jdbcTemplate.query("SELECT free_memo FROM property_memos WHERE property_id = ?",
                (rs, rowNum) -> rs.getString("free_memo"), propertyId).stream().findFirst().orElse("");
        return new PropertyMemoRow(propertyId, freeMemo, items);
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
    public void updateItem(final long propertyMemoItemId, final String content) {
        jdbcTemplate.update("UPDATE property_memo_items SET content = ? WHERE id = ?",
                content, propertyMemoItemId);
    }

    @Override
    public void saveItem(final PropertyMemoItem item) {
        jdbcTemplate.update("INSERT INTO property_memo_items "
                        + "(property_memo_id, system_meno_id, label, display_order, content) VALUES (?, ?, ?, ?, ?)",
                item.getPropertyMemoId(), item.getSystemMemoItemId(), item.getLabel(), item.getDisplayOrder(),
                item.getContent());
    }

}
