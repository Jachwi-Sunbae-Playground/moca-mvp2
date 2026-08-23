package com.jachwisunbae.property.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PropertyMemoItemRequest(
        @NotNull Long systemMemoItemId,
        @NotNull @Size(max = 100) String content) {
}
