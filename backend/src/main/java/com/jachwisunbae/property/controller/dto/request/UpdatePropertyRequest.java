package com.jachwisunbae.property.controller.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdatePropertyRequest(
        @NotNull
        String name,
        Long depositAmount,
        Long monthlyRentAmount,
        String discoverySource
) {
}
