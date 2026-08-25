package com.jachwisunbae.checklist.controller.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record UpdateUserChecklistRequest(
        String name,
        @NotNull @Size(min = 1, max = 30) List<@NotNull @Valid UserChecklistItemRequest> items) {

    public UpdateUserChecklistRequest {
        items = items == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(items));
    }
}
