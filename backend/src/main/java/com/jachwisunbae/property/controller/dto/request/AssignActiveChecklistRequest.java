package com.jachwisunbae.property.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignActiveChecklistRequest(
        @NotNull @Positive Long checklistId
) {
}
