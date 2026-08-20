package com.jachwisunbae.property.controller.dto.response;

import com.jachwisunbae.checklist.type.CheckStage;

public record PropertyChecklistStageResponse(CheckStage stage, boolean applied,
                                             Long propertyChecklistId, String checklistName,
                                             Long sourceChecklistId, PropertyProgress progress) {
}
