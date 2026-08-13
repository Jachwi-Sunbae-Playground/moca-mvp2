package com.jachwisunbae.visit.service.dto.result;

import com.jachwisunbae.visit.domain.VisitSummary;

public record VisitSummaryResult(
        int totalCount,
        int checkedCount,
        int goodCount,
        int cautionCount,
        int unconfirmedCount
) {

    public static VisitSummaryResult from(final VisitSummary summary) {
        return new VisitSummaryResult(
                summary.totalCount(),
                summary.checkedCount(),
                summary.goodCount(),
                summary.cautionCount(),
                summary.unconfirmedCount()
        );
    }
}
