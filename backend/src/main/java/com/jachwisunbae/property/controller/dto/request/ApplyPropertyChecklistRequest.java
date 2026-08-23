package com.jachwisunbae.property.controller.dto.request;

import jakarta.validation.constraints.NotNull;

public record ApplyPropertyChecklistRequest(@NotNull SourceType sourceType, Long checklistId) {

    public enum SourceType {
        USER,
        SYSTEM_DEFAULT
    }
}
