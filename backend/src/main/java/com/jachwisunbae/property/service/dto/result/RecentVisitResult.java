package com.jachwisunbae.property.service.dto.result;

import com.jachwisunbae.property.repository.RecentVisitProjection;
import java.time.Instant;

public record RecentVisitResult(
        long visitId,
        String status,
        Instant startedAt,
        Instant completedAt,
        int totalCount,
        int checkedCount,
        int goodCount,
        int cautionCount,
        int unconfirmedCount
) {

    public static RecentVisitResult from(final RecentVisitProjection projection) {
        if (projection == null) {
            return null;
        }
        return new RecentVisitResult(
                projection.visitId(),
                projection.status(),
                projection.startedAt(),
                projection.completedAt(),
                projection.totalCount(),
                projection.checkedCount(),
                projection.goodCount(),
                projection.cautionCount(),
                projection.unconfirmedCount()
        );
    }
}
