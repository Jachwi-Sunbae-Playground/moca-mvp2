package com.jachwisunbae.visit.repository;

import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.visit.domain.CheckStatus;
import java.time.Instant;

public record VisitItemStatusStateProjection(
        long visitItemId,
        CheckStage stage,
        CheckStatus status,
        long statusVersion,
        Instant statusSavedAt
) {
}
