package com.jachwisunbae.property.controller.dto.response;

import com.jachwisunbae.property.entity.Property;
import java.util.List;

public record CreatePropertyResponse(Long id, String name, Long depositAmount, Long monthlyRentAmount,
                                     String discoverySource,
                                     List<Object> photos, PropertyCreateProgress overallProgress) {
    public static CreatePropertyResponse from(final Property property) {
        return new CreatePropertyResponse(property.getId(), property.getName(), property.getDepositAmount(),
                property.getMonthlyRentAmount(), property.getDiscoverySource(),
                List.of(), new PropertyCreateProgress(0, 0, 0, 0, 0, 0));
    }
}
