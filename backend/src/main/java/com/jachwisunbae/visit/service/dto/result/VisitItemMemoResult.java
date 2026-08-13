package com.jachwisunbae.visit.service.dto.result;

import com.jachwisunbae.visit.repository.VisitItemMemoStateProjection;
import java.time.Instant;

public record VisitItemMemoResult(
        long visitItemId,
        String memo,
        long memoVersion,
        Instant memoSavedAt
) {

    public static VisitItemMemoResult from(final VisitItemMemoStateProjection item) {
        return new VisitItemMemoResult(
                item.visitItemId(),
                item.memo().value(),
                item.memoVersion(),
                item.memoSavedAt()
        );
    }
}
