package com.jachwisunbae.checklist.controller.dto.response;

import com.jachwisunbae.checklist.entity.UserChecklist;
import com.jachwisunbae.checklist.repository.query.UserChecklistItemDetail;
import com.jachwisunbae.checklist.type.CheckStage;

import java.util.List;

public record CreateUserChecklistResponse(
        Long id,
        String name,
        CheckStage stage,
        int itemCount,
        List<UserChecklistItemResponse> items) {


    public static CreateUserChecklistResponse from(final UserChecklist checklist,
                                                    final List<UserChecklistItemDetail> details) {
        return new CreateUserChecklistResponse(
                checklist.getId(), checklist.getName(), checklist.getStage(), details.size(),
                details.stream()
                        .map(detail -> UserChecklistItemResponse.from(detail.getItem()))
                        .toList());
    }
}
