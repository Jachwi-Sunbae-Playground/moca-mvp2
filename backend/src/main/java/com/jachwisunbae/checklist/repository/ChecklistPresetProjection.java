package com.jachwisunbae.checklist.repository;

import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.checklist.domain.ChecklistPresetType;
import com.jachwisunbae.checklist.service.dto.result.OrderedCheckItemResult;
import java.util.List;

public record ChecklistPresetProjection(
        ChecklistPresetType presetType,
        CheckStage stage,
        List<OrderedCheckItemResult> items
) {

    public ChecklistPresetProjection {
        items = List.copyOf(items);
    }
}
