package com.jachwisunbae.property.repository;

import java.time.Instant;

public record RecentVisitProjection(
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
}
