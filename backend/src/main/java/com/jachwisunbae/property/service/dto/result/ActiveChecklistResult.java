package com.jachwisunbae.property.service.dto.result;

import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.checklist.service.dto.result.ChecklistReferenceResult;
import com.jachwisunbae.property.repository.ActiveChecklistProjection;

public record ActiveChecklistResult(
        long propertyId,
        CheckStage stage,
        long checklistId,
        String name,
        int itemCount
) {

    public static ActiveChecklistResult from(
            final long propertyId,
            final ChecklistReferenceResult checklist
    ) {
        return new ActiveChecklistResult(
                propertyId,
                checklist.stage(),
                checklist.checklistId(),
                checklist.name(),
                checklist.itemCount()
        );
    }

    public static ActiveChecklistResult from(final ActiveChecklistProjection projection) {
        return new ActiveChecklistResult(
                projection.propertyId(),
                projection.stage(),
                projection.checklistId(),
                projection.name(),
                projection.itemCount()
        );
    }
}
