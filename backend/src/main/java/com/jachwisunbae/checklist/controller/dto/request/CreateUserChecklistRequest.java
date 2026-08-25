package com.jachwisunbae.checklist.controller.dto.request;

import com.jachwisunbae.checklist.type.CheckStage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record CreateUserChecklistRequest(
        String name,
        @NotNull CheckStage stage,
        @NotNull @Size(min = 1, max = 30) List<@NotNull @Valid UserChecklistItemRequest> items) {

    public CreateUserChecklistRequest {
        items = items == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(items));
    }
}
