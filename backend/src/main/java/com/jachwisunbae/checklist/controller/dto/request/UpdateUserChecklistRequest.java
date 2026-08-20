package com.jachwisunbae.checklist.controller.dto.request;

import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record UpdateUserChecklistRequest(
        String name,
        @Size(max = 30) List<Long> systemCheckItemIds) {

    public UpdateUserChecklistRequest {
        systemCheckItemIds = systemCheckItemIds == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(systemCheckItemIds));
    }
}
