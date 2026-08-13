package com.jachwisunbae.checklist.repository;

import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.checklist.domain.ChecklistName;
import java.time.Instant;

public record ChecklistRootProjection(
        long checklistId,
        long memberId,
        ChecklistName name,
        CheckStage stage,
        Instant createdAt,
        Instant updatedAt
) {
}
