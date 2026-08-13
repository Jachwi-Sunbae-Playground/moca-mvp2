package com.jachwisunbae.checklist.service.dto.result;

import com.jachwisunbae.checklist.domain.CheckStage;
import java.time.Instant;
import java.util.List;

public record ChecklistDetailResult(
        long checklistId,
        String name,
        CheckStage stage,
        List<ChecklistDetailItemResult> items,
        int itemCount,
        int assignedPropertyCount,
        Instant createdAt,
        Instant updatedAt
) {

    public ChecklistDetailResult {
        items = List.copyOf(items);
    }
}
