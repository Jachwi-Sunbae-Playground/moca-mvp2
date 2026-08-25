package com.jachwisunbae.checklist.controller.dto.request;

public record UserChecklistItemRequest(
        Long systemCheckItemId,
        String question) {
}
