package com.jachwisunbae.checklist.service.dto.command;

import com.jachwisunbae.checklist.domain.CheckStage;
import java.util.List;

public record CreateChecklistCommand(
        String name,
        CheckStage stage,
        List<ChecklistItemCommand> items,
        ChecklistRequestMode mode
) {

    public CreateChecklistCommand {
        items = items == null ? null : List.copyOf(items);
    }

    public CreateChecklistCommand(final String name, final CheckStage stage, final List<Long> checkItemIds) {
        this(
                name,
                stage,
                checkItemIds == null ? null : checkItemIds.stream().map(ChecklistItemCommand::provided).toList(),
                ChecklistRequestMode.LEGACY
        );
    }
}
