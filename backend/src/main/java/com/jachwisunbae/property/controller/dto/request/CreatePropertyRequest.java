package com.jachwisunbae.property.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreatePropertyRequest(
    @NotNull
    String name,
    Long depositAmount,
    Long monthlyRentAmount,
    String discoverySource,
    @Size(max = 255) String roadAddress,
    @Size(max = 255) String jibunAddress,
    BigDecimal latitude,
    BigDecimal longitude
) {
}
