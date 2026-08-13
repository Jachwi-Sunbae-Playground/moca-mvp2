package com.jachwisunbae.visit.controller.dto.response;

import com.jachwisunbae.visit.service.dto.result.VisitListItemResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record VisitListItemResponse(
        long visitId,
        String status,
        Instant startedAt,
        @Schema(nullable = true) Instant completedAt,
        VisitSummaryResponse summary
) {

    public static VisitListItemResponse from(final VisitListItemResult result) {
        return new VisitListItemResponse(
                result.visitId(),
                result.status(),
                result.startedAt(),
                result.completedAt(),
                VisitSummaryResponse.from(result.summary())
        );
    }
}
