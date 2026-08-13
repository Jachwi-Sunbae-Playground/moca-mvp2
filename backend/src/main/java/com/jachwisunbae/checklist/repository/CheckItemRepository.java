package com.jachwisunbae.checklist.repository;

import com.jachwisunbae.checklist.domain.CheckItem;
import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.exception.server.DataInconsistencyException;
import com.jachwisunbae.common.page.PageQuery;
import java.util.Collections;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class CheckItemRepository {

    private static final String COLUMNS = "id, stage, question, guide, is_active";
    private static final String FIND_ALL_ACTIVE_SQL = """
            SELECT %s
            FROM check_items
            WHERE stage = ?
              AND is_active = TRUE
            ORDER BY id
            LIMIT ? OFFSET ?
            """.formatted(COLUMNS);
    private static final String FIND_ACTIVE_BY_QUESTION_SQL = """
            SELECT %s
            FROM check_items
            WHERE stage = ?
              AND is_active = TRUE
              AND question LIKE ? ESCAPE '!'
            ORDER BY id
            LIMIT ? OFFSET ?
            """.formatted(COLUMNS);
    private static final String COUNT_ALL_ACTIVE_SQL = """
            SELECT COUNT(*)
            FROM check_items
            WHERE stage = ?
              AND is_active = TRUE
            """;
    private static final String COUNT_ACTIVE_BY_QUESTION_SQL = """
            SELECT COUNT(*)
            FROM check_items
            WHERE stage = ?
              AND is_active = TRUE
              AND question LIKE ? ESCAPE '!'
            """;
    private static final RowMapper<CheckItem> ROW_MAPPER = (resultSet, rowNumber) -> new CheckItem(
            resultSet.getLong("id"),
            CheckStage.valueOf(resultSet.getString("stage")),
            resultSet.getString("question"),
            resultSet.getString("guide"),
            resultSet.getBoolean("is_active")
    );

    private final JdbcTemplate jdbcTemplate;

    public CheckItemRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CheckItem> findAllActive(
            final CheckStage stage,
            final String query,
            final PageQuery pageQuery
    ) {
        try {
            if (query.isEmpty()) {
                return jdbcTemplate.query(
                        FIND_ALL_ACTIVE_SQL,
                        ROW_MAPPER,
                        stage.name(),
                        pageQuery.size(),
                        pageQuery.offset()
                );
            }
            return jdbcTemplate.query(
                    FIND_ACTIVE_BY_QUESTION_SQL,
                    ROW_MAPPER,
                    stage.name(),
                    likePattern(query),
                    pageQuery.size(),
                    pageQuery.offset()
            );
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public long countAllActive(final CheckStage stage, final String query) {
        try {
            final Long count = query.isEmpty()
                    ? jdbcTemplate.queryForObject(COUNT_ALL_ACTIVE_SQL, Long.class, stage.name())
                    : jdbcTemplate.queryForObject(
                            COUNT_ACTIVE_BY_QUESTION_SQL,
                            Long.class,
                            stage.name(),
                            likePattern(query)
                    );
            return count == null ? 0L : count;
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public List<CheckItem> findAllByIds(final List<Long> checkItemIds) {
        if (checkItemIds.isEmpty()) {
            return List.of();
        }
        final String placeholders = String.join(", ", Collections.nCopies(checkItemIds.size(), "?"));
        final String sql = "SELECT " + COLUMNS + " FROM check_items WHERE id IN (" + placeholders + ")";
        try {
            return jdbcTemplate.query(sql, ROW_MAPPER, checkItemIds.toArray());
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    private String likePattern(final String query) {
        final String escaped = query
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }

    private DataInconsistencyException dataInconsistency(final DataAccessException cause) {
        return new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR, cause);
    }
}
