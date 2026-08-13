package com.jachwisunbae.property.repository;

import com.jachwisunbae.property.domain.DiscoverySource;
import com.jachwisunbae.property.domain.Money;
import com.jachwisunbae.property.domain.PropertyName;
import com.jachwisunbae.property.domain.PropertyPreVisitMemo;
import java.time.Instant;

public record PropertyDetailProjection(
        long propertyId,
        PropertyName name,
        Money depositAmount,
        Money monthlyRentAmount,
        DiscoverySource discoverySource,
        PropertyPreVisitMemo memo,
        int photoCount,
        RecentVisitProjection recentVisit,
        int visitCount,
        Instant createdAt,
        Instant updatedAt,
        Instant lastActivityAt
) {
}
