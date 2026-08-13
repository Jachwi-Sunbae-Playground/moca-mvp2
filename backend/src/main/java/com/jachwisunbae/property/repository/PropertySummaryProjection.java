package com.jachwisunbae.property.repository;

import com.jachwisunbae.property.domain.DiscoverySource;
import com.jachwisunbae.property.domain.Money;
import com.jachwisunbae.property.domain.PropertyName;
import java.time.Instant;

public record PropertySummaryProjection(
        long propertyId,
        PropertyName name,
        Money depositAmount,
        Money monthlyRentAmount,
        DiscoverySource discoverySource,
        RecentVisitProjection recentVisit,
        int photoCount,
        Instant lastActivityAt
) {
}
