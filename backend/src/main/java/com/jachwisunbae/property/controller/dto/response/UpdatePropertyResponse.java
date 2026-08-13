package com.jachwisunbae.property.controller.dto.response;

import com.jachwisunbae.property.service.dto.result.PropertyResult;
import java.time.Instant;

public record UpdatePropertyResponse(
        long propertyId,
        String name,
        long depositAmount,
        long monthlyRentAmount,
        DiscoverySourceResponse discoverySource,
        Instant updatedAt
) {

    public static UpdatePropertyResponse from(final PropertyResult result) {
        return new UpdatePropertyResponse(
                result.propertyId(),
                result.name(),
                result.depositAmount(),
                result.monthlyRentAmount(),
                DiscoverySourceResponse.from(result.discoverySource()),
                result.updatedAt()
        );
    }
}
