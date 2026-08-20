package com.jachwisunbae.property.controller.dto.response;

import com.jachwisunbae.property.repository.query.PropertyChecklistApplicationQuery;
import java.util.List;

public record PropertyChecklistApplicationResponse(Long id, Long propertyId, Long sourceChecklistId,
                                                    String checklistName, String stage,
                                                    List<PropertyChecklistItemResponse> items) {
    public static PropertyChecklistApplicationResponse from(final PropertyChecklistApplicationQuery query) {
        return new PropertyChecklistApplicationResponse(query.id(), query.propertyId(), query.sourceChecklistId(),
                query.checklistName(), query.stage().name(), query.items().stream()
                        .map(PropertyChecklistItemResponse::from).toList());
    }
}
