package com.jachwisunbae.property.repository;

import com.jachwisunbae.property.entity.SystemMemoItem;
import java.util.List;
import java.util.ArrayList;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSystemMemoItemRepository implements SystemMemoItemRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<SystemMemoItem> rowMapper = (rs, rowNum) -> SystemMemoItem.reconstruct(
            rs.getLong("id"), rs.getString("label"), rs.getInt("display_order"),
            rs.getTimestamp("deleted_at") == null ? null : rs.getTimestamp("deleted_at").toLocalDateTime());

    public JdbcSystemMemoItemRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<SystemMemoItem> findActive() {
        return jdbcTemplate.query("SELECT id, label, display_order, deleted_at "
                + "FROM system_memo_items WHERE deleted_at IS NULL ORDER BY display_order, id", rowMapper);
    }

    @Override
    public List<SystemMemoItem> findActiveByIds(final List<Long> ids) {
        return findByIdsWithCondition(ids, "deleted_at IS NULL");
    }

    @Override
    public List<SystemMemoItem> findByIds(final List<Long> ids) {
        return findByIdsWithCondition(ids, "1 = 1");
    }

    private List<SystemMemoItem> findByIdsWithCondition(final List<Long> ids, final String condition) {
        if (ids.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(", ", java.util.Collections.nCopies(ids.size(), "?"));
        List<Object> parameters = new ArrayList<>(ids);
        parameters.addAll(ids);
        return jdbcTemplate.query("SELECT id, label, display_order, deleted_at FROM system_memo_items "
                        + "WHERE " + condition + " AND id IN (" + placeholders + ") "
                        + "ORDER BY FIELD(id, " + placeholders + ")",
                rowMapper, parameters.toArray());
    }
}
