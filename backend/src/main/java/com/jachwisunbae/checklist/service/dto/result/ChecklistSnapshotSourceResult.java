package com.jachwisunbae.checklist.service.dto.result;

import com.jachwisunbae.checklist.domain.CheckStage;
import java.util.List;

public record ChecklistSnapshotSourceResult(
        long checklistId,
        String name,
        CheckStage stage,
        List<ChecklistSnapshotItemResult> items
) {

    public ChecklistSnapshotSourceResult {
        items = List.copyOf(items);
    }
}
