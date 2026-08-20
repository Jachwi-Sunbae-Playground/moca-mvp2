package com.jachwisunbae.property.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePropertyChecklistMemoRequest(@NotNull @Size(max = 500) String memo) {
}
