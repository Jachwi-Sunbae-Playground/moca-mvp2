package com.jachwisunbae.checklist.controller.dto.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.checklist.service.dto.command.ChecklistItemCommand;
import com.jachwisunbae.checklist.service.dto.command.ChecklistRequestMode;
import com.jachwisunbae.checklist.service.dto.command.CreateChecklistCommand;
import com.jachwisunbae.common.exception.client.BusinessRuleViolationException;
import com.jachwisunbae.common.exception.client.InvalidCommandException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public final class CreateChecklistRequest {

    private String name;
    private String stage;
    private List<ChecklistItemRequest> items;
    private List<Long> checkItemIds;
    private boolean itemsPresent;
    private boolean checkItemIdsPresent;

    public void setName(final String name) {
        this.name = name;
    }

    public void setStage(final String stage) {
        this.stage = stage;
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

    @NotBlank
    @Schema(allowableValues = {"ONLINE_PHONE", "ON_SITE", "PRE_CONTRACT"})
    public String getStage() {
        return stage;
    }

    @Schema(description = "v1.1 PROVIDED·CUSTOM 최종 순서. checkItemIds와 함께 보낼 수 없다.")
    public List<ChecklistItemRequest> getItems() {
        return items;
    }

    @Schema(description = "v1.0 PROVIDED 전용 항목 ID 순서", deprecated = true)
    public List<Long> getCheckItemIds() {
        return checkItemIds;
    }

    public CreateChecklistCommand toCommand() {
        rejectRepresentationConflict();
        if (itemsPresent) {
            return new CreateChecklistCommand(
                    name,
                    CheckStage.from(stage),
                    toItemCommands(items, false),
                    ChecklistRequestMode.V11
            );
        }
        if (checkItemIdsPresent) {
            return new CreateChecklistCommand(
                    name,
                    CheckStage.from(stage),
                    toLegacyCommands(checkItemIds),
                    ChecklistRequestMode.LEGACY
            );
        }
        throw empty();
    }

    private void rejectRepresentationConflict() {
        if (itemsPresent && checkItemIdsPresent) {
            throw new InvalidCommandException(ErrorCode.CHECKLIST_ITEMS_REPRESENTATION_CONFLICT);
        }
    }

    static List<ChecklistItemCommand> toItemCommands(
            final List<ChecklistItemRequest> requests,
            final boolean allowExistingCustom
    ) {
        if (requests == null || requests.isEmpty()) {
            throw empty();
        }
        return requests.stream()
                .map(request -> {
                    if (request == null) {
                        throw new InvalidCommandException(ErrorCode.CUSTOM_CHECKLIST_ITEM_INVALID);
                    }
                    return request.toCommand(allowExistingCustom);
                })
                .toList();
    }

    static List<ChecklistItemCommand> toLegacyCommands(final List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw empty();
        }
        return ids.stream()
                .map(id -> {
                    if (id == null || id <= 0) {
                        throw new InvalidCommandException(ErrorCode.INVALID_REQUEST);
                    }
                    return ChecklistItemCommand.provided(id);
                })
                .toList();
    }

    private static BusinessRuleViolationException empty() {
        return new BusinessRuleViolationException(ErrorCode.CHECKLIST_EMPTY);
    }
}
