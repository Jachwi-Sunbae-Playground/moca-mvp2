package com.jachwisunbae.checklist.service.dto.result;

import com.jachwisunbae.checklist.domain.ChecklistItemOrigin;

public record ChecklistDetailItemResult(
        long checklistItemId,
        ChecklistItemOrigin origin,
        Long sourceCheckItemId,
        String question,
        String guide,
        int order
) {
}
