package com.jachwisunbae.property.controller.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdatePropertyMemoRequest(
        @NotNull @Size(max = 20) List<@NotNull @Valid PropertyMemoItemRequest> items,
        @NotNull @Size(max = 2000) String freeMemo) {
}
