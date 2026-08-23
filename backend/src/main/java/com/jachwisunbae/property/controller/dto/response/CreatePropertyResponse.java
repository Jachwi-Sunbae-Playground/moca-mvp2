package com.jachwisunbae.property.controller.dto.response;

import com.jachwisunbae.property.entity.Property;
import java.util.List;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;

public record CreatePropertyResponse(Long id, String name, Long depositAmount, Long monthlyRentAmount,
                                     String discoverySource, String address, String roadAddress,
                                     String jibunAddress, BigDecimal latitude, BigDecimal longitude,
                                     Instant createdAt, Instant updatedAt, Instant lastActivityAt,
                                     List<Object> photos, PropertyCreateProgress overallProgress) {
    public static CreatePropertyResponse from(final Property property) {
        return new CreatePropertyResponse(property.getId(), property.getName(), property.getDepositAmount(),
                property.getMonthlyRentAmount(), property.getDiscoverySource(),
                property.getAddress(), property.getRoadAddress(), property.getJibunAddress(),
                property.getLatitude(), property.getLongitude(),
                property.getCreatedAt().toInstant(ZoneOffset.UTC),
                property.getUpdatedAt().toInstant(ZoneOffset.UTC),
                property.getLastActivityAt().toInstant(ZoneOffset.UTC),
                List.of(), new PropertyCreateProgress(0, 0, 0, 0, 0, 0));
    }
}
