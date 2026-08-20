package com.jachwisunbae.checklist.controller.dto.request;

import com.jachwisunbae.checklist.type.CheckStage;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record CreateUserChecklistRequest(
        String name,
        @NotNull CheckStage stage,
        @NotNull @Size(max = 30) List<@NotNull Long> optionalSystemCheckItemIds) {

    public CreateUserChecklistRequest {
        optionalSystemCheckItemIds = optionalSystemCheckItemIds == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(optionalSystemCheckItemIds));
    }
}
