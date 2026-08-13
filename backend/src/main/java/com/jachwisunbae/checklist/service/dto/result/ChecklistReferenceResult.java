package com.jachwisunbae.checklist.service.dto.result;

import com.jachwisunbae.checklist.domain.CheckStage;

public record ChecklistReferenceResult(
        long checklistId,
        String name,
        CheckStage stage,
        int itemCount
) {
}
