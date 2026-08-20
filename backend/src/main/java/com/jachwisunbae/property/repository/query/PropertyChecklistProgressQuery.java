package com.jachwisunbae.property.repository.query;

import com.jachwisunbae.checklist.type.CheckStage;

public record PropertyChecklistProgressQuery(CheckStage stage, Long propertyChecklistId,
                                             String checklistName, Long sourceChecklistId,
                                             PropertyProgressSummary progress) {
}
