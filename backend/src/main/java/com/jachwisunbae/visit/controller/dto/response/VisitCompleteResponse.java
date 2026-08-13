package com.jachwisunbae.visit.controller.dto.response;

import com.jachwisunbae.visit.service.dto.result.VisitCompleteResult;
import java.time.Instant;

public record VisitCompleteResponse(
        long visitId,
        String status,
        Instant startedAt,
        Instant completedAt,
        VisitSummaryResponse summary
) {

    public static VisitCompleteResponse from(final VisitCompleteResult result) {
        return new VisitCompleteResponse(
                result.visitId(),
                result.status(),
                result.startedAt(),
                result.completedAt(),
                VisitSummaryResponse.from(result.summary())
        );
    }
}
