package com.jachwisunbae.checklist.controller.dto.response;

import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.checklist.service.dto.result.ChecklistDetailResult;
import java.time.Instant;
import java.util.List;

public record ChecklistDetailResponse(
        long checklistId,
        String name,
        CheckStage stage,
        List<ChecklistItemResponse> items,
        int itemCount,
        int assignedPropertyCount,
        Instant createdAt,
        Instant updatedAt
) {

    public static ChecklistDetailResponse from(final ChecklistDetailResult result) {
        return new ChecklistDetailResponse(
                result.checklistId(),
                result.name(),
                result.stage(),
                result.items().stream().map(ChecklistItemResponse::from).toList(),
                result.itemCount(),
                result.assignedPropertyCount(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
