package com.jachwisunbae.property.controller.dto.response;

import com.jachwisunbae.property.service.dto.result.PropertySummaryResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record PropertySummaryResponse(
        long propertyId,
        String name,
        long depositAmount,
        long monthlyRentAmount,
        DiscoverySourceResponse discoverySource,
        @Schema(nullable = true) RecentVisitResponse recentVisit,
        int photoCount,
        Instant lastActivityAt
) {

    public static PropertySummaryResponse from(final PropertySummaryResult result) {
        return new PropertySummaryResponse(
                result.propertyId(),
                result.name(),
                result.depositAmount(),
                result.monthlyRentAmount(),
                DiscoverySourceResponse.from(result.discoverySource()),
                RecentVisitResponse.from(result.recentVisit()),
                result.photoCount(),
                result.lastActivityAt()
        );
    }

    public record RecentVisitResponse(
            long visitId,
            String status,
            Instant startedAt,
            Instant completedAt,
            VisitSummaryResponse summary
    ) {

        static RecentVisitResponse from(
                final com.jachwisunbae.property.service.dto.result.RecentVisitResult result
        ) {
            if (result == null) {
                return null;
            }
            return new RecentVisitResponse(
                    result.visitId(),
                    result.status(),
                    result.startedAt(),
                    result.completedAt(),
                    new VisitSummaryResponse(
                            result.totalCount(),
                            result.checkedCount(),
                            result.goodCount(),
                            result.cautionCount(),
                            result.unconfirmedCount()
                    )
            );
        }
    }

    public record VisitSummaryResponse(
            int totalCount,
            int checkedCount,
            int goodCount,
            int cautionCount,
            int unconfirmedCount
    ) {
    }
}
