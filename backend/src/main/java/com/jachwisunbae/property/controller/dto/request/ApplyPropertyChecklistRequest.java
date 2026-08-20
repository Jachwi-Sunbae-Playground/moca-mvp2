package com.jachwisunbae.property.controller.dto.request;

import jakarta.validation.constraints.NotNull;

public record ApplyPropertyChecklistRequest(@NotNull Long checklistId) {
}
