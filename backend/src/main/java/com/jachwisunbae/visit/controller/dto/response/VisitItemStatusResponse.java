package com.jachwisunbae.visit.controller.dto.response;

import com.jachwisunbae.visit.service.dto.result.VisitItemStatusResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record VisitItemStatusResponse(
        ItemResponse item,
        VisitSummaryResponse stageSummary,
        VisitSummaryResponse visitSummary
) {

    public static VisitItemStatusResponse from(final VisitItemStatusResult result) {
        return new VisitItemStatusResponse(
                new ItemResponse(
                        result.item().visitItemId(),
                        result.item().status(),
                        result.item().statusVersion(),
                        result.item().statusSavedAt(),
                        result.item().statusVersion(),
                        result.item().statusSavedAt()
                ),
                VisitSummaryResponse.from(result.stageSummary()),
                VisitSummaryResponse.from(result.visitSummary())
        );
    }

    @Schema(name = "VisitItemStatusItemResponse")
    public record ItemResponse(
            long visitItemId,
            String status,
            long statusVersion,
            Instant statusSavedAt,
            @Schema(deprecated = true) long version,
            @Schema(deprecated = true) Instant savedAt
    ) {
    }
}
