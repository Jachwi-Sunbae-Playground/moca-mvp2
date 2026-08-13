package com.jachwisunbae.property.service.dto.command;

public record CreatePropertyCommand(
        String name,
        long depositAmount,
        long monthlyRentAmount,
        String discoverySource
) {
}
