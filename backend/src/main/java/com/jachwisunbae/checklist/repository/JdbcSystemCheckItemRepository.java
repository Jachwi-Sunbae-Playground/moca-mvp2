package com.jachwisunbae.checklist.repository;

import com.jachwisunbae.checklist.entity.SystemCheckItem;
import com.jachwisunbae.checklist.type.CheckStage;
import com.jachwisunbae.checklist.type.CheckItemType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcSystemCheckItemRepository implements SystemCheckItemRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcSystemCheckItemRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<SystemCheckItem> findActiveByStage(final CheckStage stage, final String question) {
        String sql = """
                SELECT id, stage, item_type, question, deleted_at
                FROM system_check_items
                WHERE stage = ?
                  AND deleted_at IS NULL
                  AND (? IS NULL OR question LIKE CONCAT('%', ?, '%'))
                ORDER BY CASE item_type WHEN 'CORE' THEN 0 ELSE 1 END, id
                """;

        return jdbcTemplate.query(sql, (resultSet, rowNumber) -> SystemCheckItem.reconstruct(
                resultSet.getLong("id"),
                CheckStage.valueOf(resultSet.getString("stage")),
                CheckItemType.valueOf(resultSet.getString("item_type")),
                resultSet.getString("question"),
                resultSet.getTimestamp("deleted_at") == null
                        ? null
                        : resultSet.getTimestamp("deleted_at").toLocalDateTime()
        ), stage.name(), question, question);
    }
}
