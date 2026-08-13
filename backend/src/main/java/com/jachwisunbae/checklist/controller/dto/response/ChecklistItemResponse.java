package com.jachwisunbae.checklist.controller.dto.response;

import com.jachwisunbae.checklist.domain.ChecklistItemOrigin;
import com.jachwisunbae.checklist.service.dto.result.ChecklistDetailItemResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record ChecklistItemResponse(
        long checklistItemId,
        ChecklistItemOrigin origin,
        @Schema(nullable = true) Long sourceCheckItemId,
        @Schema(nullable = true, deprecated = true) Long checkItemId,
        String question,
        @Schema(nullable = true) String guide,
        int order
) {

    public static ChecklistItemResponse from(final ChecklistDetailItemResult result) {
        return new ChecklistItemResponse(
                result.checklistItemId(),
                result.origin(),
                result.sourceCheckItemId(),
                result.sourceCheckItemId(),
                result.question(),
                result.guide(),
                result.order()
        );
    }
}
