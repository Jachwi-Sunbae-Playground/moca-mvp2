package com.jachwisunbae.property.controller.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreatePropertyRequest(
    @NotNull
    String name,
    Long depositAmount,
    Long monthlyRentAmount,
    String discoverySource
) {
}
