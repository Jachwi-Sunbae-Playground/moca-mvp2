package com.jachwisunbae.property.controller.dto.response;

import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.property.service.dto.result.ActiveChecklistResult;

public record ActiveChecklistResponse(
        long propertyId,
        CheckStage stage,
        long checklistId,
        String name,
        int itemCount
) {

    public static ActiveChecklistResponse from(final ActiveChecklistResult result) {
        return new ActiveChecklistResponse(
                result.propertyId(),
                result.stage(),
                result.checklistId(),
                result.name(),
                result.itemCount()
        );
    }
}
