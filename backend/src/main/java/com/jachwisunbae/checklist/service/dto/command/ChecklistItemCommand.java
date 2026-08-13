package com.jachwisunbae.checklist.service.dto.command;

import com.jachwisunbae.checklist.domain.ChecklistItem;
import com.jachwisunbae.checklist.domain.ChecklistItemOrigin;
import com.jachwisunbae.common.exception.client.InvalidCommandException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;

public record ChecklistItemCommand(
        ChecklistItemOrigin origin,
        Long sourceCheckItemId,
        Long checklistItemId,
        String question
) {

    public ChecklistItemCommand {
        if (origin == ChecklistItemOrigin.PROVIDED) {
            if (sourceCheckItemId == null || sourceCheckItemId <= 0 || checklistItemId != null || question != null) {
                throw invalid();
            }
        } else if (origin == ChecklistItemOrigin.CUSTOM) {
            final String normalized = ChecklistItem.normalizeCustomQuestion(question);
            if (sourceCheckItemId != null || (checklistItemId != null && checklistItemId <= 0) || normalized == null) {
                throw invalid();
            }
            question = normalized;
        } else {
            throw invalid();
        }
    }

    public static ChecklistItemCommand provided(final long sourceCheckItemId) {
        return new ChecklistItemCommand(ChecklistItemOrigin.PROVIDED, sourceCheckItemId, null, null);
    }

    public static ChecklistItemCommand custom(final Long checklistItemId, final String question) {
        return new ChecklistItemCommand(ChecklistItemOrigin.CUSTOM, null, checklistItemId, question);
    }

    private static InvalidCommandException invalid() {
        return new InvalidCommandException(ErrorCode.CUSTOM_CHECKLIST_ITEM_INVALID);
    }
}
