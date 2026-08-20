package com.jachwisunbae.checklist.controller.dto.request;

import com.jachwisunbae.checklist.type.CheckStage;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SystemCheckItemSearchRequest(
        @NotNull CheckStage stage,
        @Size(max = 200) String query) {

    public SystemCheckItemSearchRequest {
        query = normalizeQuery(query);
    }

    private static String normalizeQuery(final String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        return query.trim();
    }
}
