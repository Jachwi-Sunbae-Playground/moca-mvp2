package com.jachwisunbae.property.controller.dto.response;

import com.jachwisunbae.property.entity.Property;

public record UpdatePropertyResponse(
        Long id,
        String name,
        Long depositAmount,
        Long monthlyRentAmount,
        String discoverySource
){
    public static UpdatePropertyResponse from(final Property property) {
        return new UpdatePropertyResponse(
                property.getId(),
                property.getName(),
                property.getDepositAmount(),
                property.getMonthlyRentAmount(),
                property.getDiscoverySource()
        );
    }
}
