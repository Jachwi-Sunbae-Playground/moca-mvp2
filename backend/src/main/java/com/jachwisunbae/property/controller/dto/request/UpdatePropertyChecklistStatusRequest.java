package com.jachwisunbae.property.controller.dto.request;

import com.jachwisunbae.checklist.type.CheckStatus;
import jakarta.validation.constraints.NotNull;

public record UpdatePropertyChecklistStatusRequest(@NotNull CheckStatus status) {
}
