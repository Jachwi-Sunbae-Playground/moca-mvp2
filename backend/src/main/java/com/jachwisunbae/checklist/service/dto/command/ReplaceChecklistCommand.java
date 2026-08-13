package com.jachwisunbae.checklist.service.dto.command;

import java.util.List;

public record ReplaceChecklistCommand(
        String name,
        List<ChecklistItemCommand> items,
        ChecklistRequestMode mode
) {

    public ReplaceChecklistCommand {
        items = items == null ? null : List.copyOf(items);
    }

    public ReplaceChecklistCommand(final String name, final List<Long> checkItemIds) {
        this(
                name,
                checkItemIds == null ? null : checkItemIds.stream().map(ChecklistItemCommand::provided).toList(),
                ChecklistRequestMode.LEGACY
        );
    }
}
