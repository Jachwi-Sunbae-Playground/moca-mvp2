package com.jachwisunbae.checklist.controller.dto.response;

import com.jachwisunbae.checklist.entity.UserChecklist;
import com.jachwisunbae.checklist.type.CheckStage;

public record UserChecklistSummaryResponse(Long id, String name, CheckStage stage, int itemCount) {
    public static UserChecklistSummaryResponse from(final UserChecklist checklist, final int itemCount) {
        return new UserChecklistSummaryResponse(checklist.getId(), checklist.getName(), checklist.getStage(), itemCount);
    }
}
