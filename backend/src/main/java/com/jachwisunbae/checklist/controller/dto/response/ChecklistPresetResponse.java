package com.jachwisunbae.checklist.controller.dto.response;

import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.checklist.domain.ChecklistPresetType;
import com.jachwisunbae.checklist.service.dto.result.ChecklistPresetResult;
import java.util.List;

public record ChecklistPresetResponse(
        ChecklistPresetType presetType,
        CheckStage stage,
        List<OrderedCheckItemResponse> items
) {

    public static ChecklistPresetResponse from(final ChecklistPresetResult result) {
        return new ChecklistPresetResponse(
                result.presetType(),
                result.stage(),
                result.items().stream().map(OrderedCheckItemResponse::from).toList()
        );
    }
}
