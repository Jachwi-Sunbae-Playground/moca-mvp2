package com.jachwisunbae.visit.service.dto.result;

import com.jachwisunbae.visit.domain.Visit;
import java.time.Instant;

public record VisitCompleteResult(
        long visitId,
        String status,
        Instant startedAt,
        Instant completedAt,
        VisitSummaryResult summary
) {

    public static VisitCompleteResult from(final Visit visit, final VisitSummaryResult summary) {
        return new VisitCompleteResult(
                visit.id(),
                visit.status().name(),
                visit.startedAt(),
                visit.completedAt(),
                summary
        );
    }
}
