package com.jachwisunbae.checklist.controller.dto.request;

import com.jachwisunbae.checklist.domain.ChecklistItemOrigin;
import com.jachwisunbae.checklist.service.dto.command.ChecklistItemCommand;
import com.jachwisunbae.common.exception.client.InvalidCommandException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;

public record ChecklistItemRequest(
        @Schema(allowableValues = {"PROVIDED", "CUSTOM"})
        String origin,
        @Schema(nullable = true, description = "PROVIDED 전역 항목 ID")
        Long sourceCheckItemId,
        @Schema(nullable = true, description = "기존 CUSTOM 로컬 항목 ID. 전체 변경에서만 사용한다.")
        Long checklistItemId,
        @Schema(nullable = true, maxLength = 200, description = "CUSTOM 표시 질문")
        String question
) {

    public ChecklistItemCommand toCommand(final boolean allowExistingCustom) {
        final ChecklistItemOrigin parsedOrigin = parseOrigin();
        if (parsedOrigin == ChecklistItemOrigin.PROVIDED) {
            return ChecklistItemCommand.provided(requirePositiveSourceId());
        }
        if (sourceCheckItemId != null) {
            throw invalid();
        }
        if (!allowExistingCustom && checklistItemId != null) {
            throw invalid();
        }
        return ChecklistItemCommand.custom(checklistItemId, question);
    }

    private ChecklistItemOrigin parseOrigin() {
        try {
            return ChecklistItemOrigin.valueOf(origin);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw invalid();
        }
    }

    private long requirePositiveSourceId() {
        if (sourceCheckItemId == null || sourceCheckItemId <= 0 || checklistItemId != null || question != null) {
            throw invalid();
        }
        return sourceCheckItemId;
    }

    private InvalidCommandException invalid() {
        return new InvalidCommandException(ErrorCode.CUSTOM_CHECKLIST_ITEM_INVALID);
    }
}
