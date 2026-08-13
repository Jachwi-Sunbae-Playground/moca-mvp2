package com.jachwisunbae.property.repository;

import com.jachwisunbae.checklist.domain.CheckStage;

public record ActiveChecklistProjection(
        long propertyId,
        CheckStage stage,
        long checklistId,
        String name,
        int itemCount
) {
}
