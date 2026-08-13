package com.jachwisunbae.visit.controller.dto.response;

import com.jachwisunbae.visit.service.dto.result.VisitItemMemoResult;
import java.time.Instant;

public record VisitItemMemoResponse(
        long visitItemId,
        String memo,
        long memoVersion,
        Instant memoSavedAt
) {

    public static VisitItemMemoResponse from(final VisitItemMemoResult result) {
        return new VisitItemMemoResponse(
                result.visitItemId(),
                result.memo(),
                result.memoVersion(),
                result.memoSavedAt()
        );
    }
}
