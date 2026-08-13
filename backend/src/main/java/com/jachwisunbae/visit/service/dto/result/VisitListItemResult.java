package com.jachwisunbae.visit.service.dto.result;

import com.jachwisunbae.visit.repository.VisitListProjection;
import java.time.Instant;

public record VisitListItemResult(
        long visitId,
        String status,
        Instant startedAt,
        Instant completedAt,
        VisitSummaryResult summary
) {

    public static VisitListItemResult from(final VisitListProjection projection) {
        return new VisitListItemResult(
                projection.visitId(),
                projection.status().name(),
                projection.startedAt(),
                projection.completedAt(),
                VisitSummaryResult.from(projection.summary())
        );
    }
}
