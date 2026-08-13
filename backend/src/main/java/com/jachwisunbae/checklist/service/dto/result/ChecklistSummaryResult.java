package com.jachwisunbae.checklist.service.dto.result;

import com.jachwisunbae.checklist.domain.CheckStage;
import java.time.Instant;

public record ChecklistSummaryResult(
        long checklistId,
        String name,
        CheckStage stage,
        int itemCount,
        int assignedPropertyCount,
        Instant updatedAt
) {
}
