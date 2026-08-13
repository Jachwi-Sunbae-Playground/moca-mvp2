package com.jachwisunbae.property.service.dto.result;

import com.jachwisunbae.property.domain.DiscoverySource;
import com.jachwisunbae.property.repository.PropertySummaryProjection;
import java.time.Instant;

public record PropertySummaryResult(
        long propertyId,
        String name,
        long depositAmount,
        long monthlyRentAmount,
        DiscoverySource discoverySource,
        RecentVisitResult recentVisit,
        int photoCount,
        Instant lastActivityAt
) {

    public static PropertySummaryResult from(final PropertySummaryProjection projection) {
        return new PropertySummaryResult(
                projection.propertyId(),
                projection.name().value(),
                projection.depositAmount().amount(),
                projection.monthlyRentAmount().amount(),
                projection.discoverySource(),
                RecentVisitResult.from(projection.recentVisit()),
                projection.photoCount(),
                projection.lastActivityAt()
        );
    }
}
