package com.jachwisunbae.property.repository.query;

import com.jachwisunbae.checklist.type.CheckStage;
import java.util.List;

public record PropertyChecklistApplicationQuery(Long id, Long propertyId, Long sourceChecklistId,
                                                String checklistName, CheckStage stage,
                                                List<PropertyChecklistItemQuery> items) {
}
