package com.jachwisunbae.checklist.controller.dto.response;

import com.jachwisunbae.checklist.entity.SystemCheckItem;
import com.jachwisunbae.checklist.entity.UserChecklistItem;
import com.jachwisunbae.checklist.type.CheckItemType;
import com.jachwisunbae.checklist.type.ChecklistItemOrigin;

public record UserChecklistItemResponse(
        Long id,
        ChecklistItemOrigin origin,
        Long systemCheckItemId,
        CheckItemType itemType,
        String question,
        Integer displayOrder,
        boolean active) {

    public static UserChecklistItemResponse from(final UserChecklistItem item,
                                                  final SystemCheckItem systemCheckItem) {
        return new UserChecklistItemResponse(
                item.getId(), ChecklistItemOrigin.PROVIDED,
                item.getSystemCheckItemId(),
                systemCheckItem.getItemType(),
                systemCheckItem.getQuestion(),
                item.getDisplayOrder(),
                systemCheckItem.getDeletedAt() == null);
    }

    public static UserChecklistItemResponse from(final UserChecklistItem item) {
        return new UserChecklistItemResponse(item.getId(),
                item.isCustom() ? ChecklistItemOrigin.CUSTOM : ChecklistItemOrigin.PROVIDED,
                item.getSystemCheckItemId(), item.getItemType(),
                item.getQuestion(), item.getDisplayOrder(), true);
    }
}
