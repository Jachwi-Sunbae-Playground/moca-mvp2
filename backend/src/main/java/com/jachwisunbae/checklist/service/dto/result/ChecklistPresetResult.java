package com.jachwisunbae.checklist.service.dto.result;

import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.checklist.domain.ChecklistPresetType;
import java.util.List;

public record ChecklistPresetResult(
        ChecklistPresetType presetType,
        CheckStage stage,
        List<OrderedCheckItemResult> items
) {

    public ChecklistPresetResult {
        items = List.copyOf(items);
    }
}
