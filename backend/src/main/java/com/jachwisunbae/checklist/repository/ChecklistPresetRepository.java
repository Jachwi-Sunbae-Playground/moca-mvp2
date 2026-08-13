package com.jachwisunbae.checklist.repository;

import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.checklist.domain.ChecklistPresetType;
import com.jachwisunbae.checklist.service.dto.result.OrderedCheckItemResult;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.exception.server.DataInconsistencyException;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ChecklistPresetRepository {

    private static final String FIND_ACTIVE_SQL = """
            SELECT preset.preset_type,
                   preset.stage,
                   item.id AS check_item_id,
                   item.question,
                   item.guide,
                   preset_item.item_order
            FROM checklist_presets preset
            JOIN checklist_preset_items preset_item ON preset_item.preset_id = preset.id
            JOIN check_items item ON item.id = preset_item.check_item_id
                                 AND item.stage = preset_item.stage
            WHERE preset.preset_type = ?
              AND preset.stage = ?
              AND preset.is_active = TRUE
              AND item.is_active = TRUE
            ORDER BY preset_item.item_order
            """;

    private final JdbcTemplate jdbcTemplate;

    public ChecklistPresetRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<ChecklistPresetProjection> findActive(
            final ChecklistPresetType presetType,
            final CheckStage stage
    ) {
        try {
            final List<OrderedCheckItemResult> items = jdbcTemplate.query(
                    FIND_ACTIVE_SQL,
                    (resultSet, rowNumber) -> new OrderedCheckItemResult(
                            resultSet.getLong("check_item_id"),
                            resultSet.getString("question"),
                            resultSet.getString("guide"),
                            rowNumber + 1
                    ),
                    presetType.name(),
                    stage.name()
            );
            return items.isEmpty()
                    ? Optional.empty()
                    : Optional.of(new ChecklistPresetProjection(presetType, stage, items));
        } catch (DataAccessException exception) {
            throw new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR, exception);
        }
    }
}
