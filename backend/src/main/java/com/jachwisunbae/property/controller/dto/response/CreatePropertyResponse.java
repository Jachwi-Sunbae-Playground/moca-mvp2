package com.jachwisunbae.property.controller.dto.response;

import com.jachwisunbae.property.service.dto.result.PropertyResult;
import java.time.Instant;

public record CreatePropertyResponse(
        long propertyId,
        String name,
        long depositAmount,
        long monthlyRentAmount,
        DiscoverySourceResponse discoverySource,
        Instant createdAt
) {

    public static CreatePropertyResponse from(final PropertyResult result) {
        return new CreatePropertyResponse(
                result.propertyId(),
                result.name(),
                result.depositAmount(),
                result.monthlyRentAmount(),
                DiscoverySourceResponse.from(result.discoverySource()),
                result.createdAt()
        );
    }
}
