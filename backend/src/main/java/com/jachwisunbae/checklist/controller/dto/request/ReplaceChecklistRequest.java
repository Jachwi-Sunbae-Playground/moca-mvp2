package com.jachwisunbae.checklist.controller.dto.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.jachwisunbae.checklist.service.dto.command.ChecklistRequestMode;
import com.jachwisunbae.checklist.service.dto.command.ReplaceChecklistCommand;
import com.jachwisunbae.common.exception.client.BusinessRuleViolationException;
import com.jachwisunbae.common.exception.client.InvalidCommandException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public final class ReplaceChecklistRequest {

    private String name;
    private List<ChecklistItemRequest> items;
    private List<Long> checkItemIds;
    private boolean itemsPresent;
    private boolean checkItemIdsPresent;

    public void setName(final String name) {
        this.name = name;
    }

    @JsonSetter("items")
    public void setItems(final List<ChecklistItemRequest> items) {
        this.items = items;
        this.itemsPresent = true;
    }

    @JsonSetter("checkItemIds")
    public void setCheckItemIds(final List<Long> checkItemIds) {
        this.checkItemIds = checkItemIds;
        this.checkItemIdsPresent = true;
    }

    @NotBlank
    @Schema(maxLength = 50)
    public String getName() {
        return name;
    }

    @Schema(description = "v1.1 PROVIDED·CUSTOM 최종 순서. checkItemIds와 함께 보낼 수 없다.")
    public List<ChecklistItemRequest> getItems() {
        return items;
    }

    @Schema(description = "v1.0 PROVIDED 전용 항목 ID 순서", deprecated = true)
    public List<Long> getCheckItemIds() {
        return checkItemIds;
    }

    public ReplaceChecklistCommand toCommand() {
        if (itemsPresent && checkItemIdsPresent) {
            throw new InvalidCommandException(ErrorCode.CHECKLIST_ITEMS_REPRESENTATION_CONFLICT);
        }
        if (itemsPresent) {
            return new ReplaceChecklistCommand(
                    name,
                    CreateChecklistRequest.toItemCommands(items, true),
                    ChecklistRequestMode.V11
            );
        }
        if (checkItemIdsPresent) {
            return new ReplaceChecklistCommand(
                    name,
                    CreateChecklistRequest.toLegacyCommands(checkItemIds),
                    ChecklistRequestMode.LEGACY
            );
        }
        throw new BusinessRuleViolationException(ErrorCode.CHECKLIST_EMPTY);
    }
}
