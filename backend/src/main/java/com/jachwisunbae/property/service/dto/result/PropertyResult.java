package com.jachwisunbae.property.service.dto.result;

import com.jachwisunbae.property.domain.DiscoverySource;
import com.jachwisunbae.property.domain.Property;
import java.time.Instant;

public record PropertyResult(
        long propertyId,
        String name,
        long depositAmount,
        long monthlyRentAmount,
        DiscoverySource discoverySource,
        Instant createdAt,
        Instant updatedAt
) {

    public static PropertyResult from(final Property property) {
        return new PropertyResult(
                property.id(),
                property.name().value(),
                property.depositAmount().amount(),
                property.monthlyRentAmount().amount(),
                property.discoverySource(),
                property.createdAt(),
                property.updatedAt()
        );
    }
}
