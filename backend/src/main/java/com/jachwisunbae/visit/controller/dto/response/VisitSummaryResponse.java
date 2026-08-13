package com.jachwisunbae.visit.controller.dto.response;

import com.jachwisunbae.visit.service.dto.result.VisitSummaryResult;

public record VisitSummaryResponse(
        int totalCount,
        int checkedCount,
        int goodCount,
        int cautionCount,
        int unconfirmedCount
) {

    public static VisitSummaryResponse from(final VisitSummaryResult result) {
        return new VisitSummaryResponse(
                result.totalCount(),
                result.checkedCount(),
                result.goodCount(),
                result.cautionCount(),
                result.unconfirmedCount()
        );
    }
}
