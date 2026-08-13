package com.jachwisunbae.visit.service.dto.result;

import com.jachwisunbae.visit.repository.VisitItemStatusStateProjection;
import java.time.Instant;

public record VisitItemStatusResult(
        ItemResult item,
        VisitSummaryResult stageSummary,
        VisitSummaryResult visitSummary
) {

    public static VisitItemStatusResult from(
            final VisitItemStatusStateProjection item,
            final VisitSummaryResult stageSummary,
            final VisitSummaryResult visitSummary
    ) {
        return new VisitItemStatusResult(
                new ItemResult(
                        item.visitItemId(),
                        item.status().name(),
                        item.statusVersion(),
                        item.statusSavedAt()
                ),
                stageSummary,
                visitSummary
        );
    }

    public record ItemResult(
            long visitItemId,
            String status,
            long statusVersion,
            Instant statusSavedAt
    ) {
    }
}
