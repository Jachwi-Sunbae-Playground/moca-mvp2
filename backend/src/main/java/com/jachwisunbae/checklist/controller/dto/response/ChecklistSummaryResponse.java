package com.jachwisunbae.checklist.controller.dto.response;

import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.checklist.service.dto.result.ChecklistSummaryResult;
import java.time.Instant;

public record ChecklistSummaryResponse(
        long checklistId,
        String name,
        CheckStage stage,
        int itemCount,
        int assignedPropertyCount,
        Instant updatedAt
) {

    public static ChecklistSummaryResponse from(final ChecklistSummaryResult result) {
        return new ChecklistSummaryResponse(
                result.checklistId(),
                result.name(),
                result.stage(),
                result.itemCount(),
                result.assignedPropertyCount(),
                result.updatedAt()
        );
    }
}
