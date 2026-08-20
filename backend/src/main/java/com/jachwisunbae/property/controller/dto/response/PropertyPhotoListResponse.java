package com.jachwisunbae.property.controller.dto.response;

import java.util.List;

public record PropertyPhotoListResponse(Long propertyId, int totalCount, List<PropertyPhotoResponse> items) {
}
