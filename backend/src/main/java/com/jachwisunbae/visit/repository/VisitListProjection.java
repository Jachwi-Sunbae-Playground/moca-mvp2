package com.jachwisunbae.visit.repository;

import com.jachwisunbae.visit.domain.VisitStatus;
import com.jachwisunbae.visit.domain.VisitSummary;
import java.time.Instant;

public record VisitListProjection(
        long visitId,
        VisitStatus status,
        Instant startedAt,
        Instant completedAt,
        VisitSummary summary
) {
}
