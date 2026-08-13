package com.jachwisunbae.checklist.repository;

import com.jachwisunbae.checklist.domain.ChecklistItemOrigin;

public record ChecklistSnapshotItemProjection(
        long checklistId,
        long checklistItemId,
        ChecklistItemOrigin origin,
        Long sourceCheckItemId,
        String question,
        String guide,
        int order
) {
}
