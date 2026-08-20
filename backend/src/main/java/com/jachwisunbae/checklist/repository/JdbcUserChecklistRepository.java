package com.jachwisunbae.checklist.repository;

import com.jachwisunbae.checklist.entity.UserChecklist;
import com.jachwisunbae.checklist.entity.UserChecklistItem;
import com.jachwisunbae.checklist.entity.SystemCheckItem;
import com.jachwisunbae.checklist.repository.query.UserChecklistItemDetail;
import com.jachwisunbae.checklist.type.CheckStage;
import com.jachwisunbae.checklist.type.CheckItemType;
import java.time.LocalDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcUserChecklistRepository implements UserChecklistRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<UserChecklist> checklistRowMapper = (rs, row) -> UserChecklist.reconstruct(
            rs.getLong("id"), rs.getLong("member_id"), rs.getString("name"),
            CheckStage.valueOf(rs.getString("stage")));
    private final RowMapper<UserChecklistItem> itemRowMapper = (rs, row) -> UserChecklistItem.reconstruct(
            rs.getLong("id"), rs.getLong("user_checklist_id"),
            rs.getLong("system_check_item_id"), rs.getInt("display_order"));
    private final RowMapper<UserChecklistItemDetail> itemDetailRowMapper = (rs, row) ->
            new UserChecklistItemDetail(
                    UserChecklistItem.reconstruct(rs.getLong("id"), rs.getLong("user_checklist_id"),
                            rs.getLong("system_check_item_id"), rs.getInt("display_order")),
                    SystemCheckItem.reconstruct(rs.getLong("s_id"), CheckStage.valueOf(rs.getString("stage")),
                            CheckItemType.valueOf(rs.getString("item_type")), rs.getString("question"),
                            deletedAt(rs.getTimestamp("deleted_at"))));

    public JdbcUserChecklistRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public UserChecklist save(final UserChecklist checklist) {
        String sql = "INSERT INTO user_checklists (member_id, name, stage) VALUES (?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, checklist.getMemberId());
            statement.setString(2, checklist.getName());
            statement.setString(3, checklist.getStage().name());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("체크리스트 ID를 생성하지 못했습니다.");
        }
        return UserChecklist.reconstruct(key.longValue(), checklist.getMemberId(), checklist.getName(),
                checklist.getStage());
    }

    @Override
    public void saveItems(final long checklistId, final List<UserChecklistItem> items) {
        String sql = """
                INSERT INTO user_checklist_items
                    (user_checklist_id, system_check_item_id, display_order)
                VALUES (?, ?, ?)
                """;
        List<Object[]> parameters = items.stream()
                .map(item -> new Object[]{
                        checklistId,
                        item.getSystemCheckItemId(),
                        item.getDisplayOrder()
                })
                .toList();
        jdbcTemplate.batchUpdate(sql, parameters);
    }

    @Override
    public Optional<UserChecklist> findByIdAndMemberId(final long checklistId, final long memberId) {
        return findOne("SELECT id, member_id, name, stage FROM user_checklists WHERE id = ? AND member_id = ?",
                checklistId, memberId);
    }

    @Override
    public Optional<UserChecklist> findByIdAndMemberIdForUpdate(final long checklistId, final long memberId) {
        return findOne("""
                SELECT id, member_id, name, stage
                FROM user_checklists
                WHERE id = ? AND member_id = ?
                FOR UPDATE
                """, checklistId, memberId);
    }

    private Optional<UserChecklist> findOne(final String sql, final long checklistId, final long memberId) {
        return jdbcTemplate.query(sql, checklistRowMapper, checklistId, memberId)
                .stream().findFirst();
    }

    @Override
    public List<UserChecklist> findByMemberId(final long memberId, final CheckStage stage) {
        String sql = """
                SELECT id, member_id, name, stage FROM user_checklists
                WHERE member_id = ? AND (? IS NULL OR stage = ?) ORDER BY id DESC
                """;
        return jdbcTemplate.query(sql, checklistRowMapper, memberId,
                stageName(stage), stageName(stage));
    }

    @Override
    public List<UserChecklistItem> findItems(final long checklistId) {
        String sql = """
                SELECT id, user_checklist_id, system_check_item_id, display_order
                FROM user_checklist_items WHERE user_checklist_id = ? ORDER BY display_order
                """;
        return jdbcTemplate.query(sql, itemRowMapper, checklistId);
    }

    @Override
    public List<UserChecklistItemDetail> findItemDetails(final long checklistId) {
        String sql = """
                SELECT u.id, u.user_checklist_id, u.system_check_item_id, u.display_order,
                       s.id AS s_id, s.stage, s.item_type, s.question, s.deleted_at
                FROM user_checklist_items u
                JOIN system_check_items s ON s.id = u.system_check_item_id
                WHERE u.user_checklist_id = ?
                ORDER BY u.display_order
                """;
        return jdbcTemplate.query(sql, itemDetailRowMapper, checklistId);
    }

    private String stageName(final CheckStage stage) {
        if(stage == null)
            return null;
        return stage.name();
    }

    private LocalDateTime deletedAt(final java.sql.Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toLocalDateTime();
    }

    @Override
    public void updateName(final long checklistId, final String name) {
        jdbcTemplate.update("UPDATE user_checklists SET name = ? WHERE id = ?", name, checklistId);
    }

    @Override
    public void deleteItems(final long checklistId) {
        jdbcTemplate.update("DELETE FROM user_checklist_items WHERE user_checklist_id = ?", checklistId);
    }

    @Override
    public void delete(final long checklistId) {
        jdbcTemplate.update("DELETE FROM user_checklists WHERE id = ?", checklistId);
    }
}
