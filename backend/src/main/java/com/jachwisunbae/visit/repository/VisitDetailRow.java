package com.jachwisunbae.visit.repository;

import com.jachwisunbae.checklist.domain.ChecklistItemOrigin;
import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.visit.domain.CheckStatus;
import com.jachwisunbae.visit.domain.VisitStatus;
import java.time.Instant;

public record VisitDetailRow(
        long visitId,
        long propertyId,
        VisitStatus visitStatus,
        Instant startedAt,
        Instant completedAt,
        Instant updatedAt,
        long snapshotId,
        CheckStage stage,
        Long sourceChecklistId,
        String checklistName,
        long visitItemId,
        ChecklistItemOrigin origin,
        Long sourceChecklistItemId,
        Long sourceCheckItemId,
        String question,
        String guide,
        int order,
        CheckStatus itemStatus,
        long statusVersion,
        Instant statusSavedAt,
        String inlineMemo,
        long memoVersion,
        Instant memoSavedAt
) {
}
