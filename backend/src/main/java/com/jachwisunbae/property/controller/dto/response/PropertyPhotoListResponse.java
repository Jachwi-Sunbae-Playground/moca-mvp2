package com.jachwisunbae.property.controller.dto.response;

import com.jachwisunbae.property.service.dto.result.PropertyPhotoListResult;
import java.util.List;

public record PropertyPhotoListResponse(List<PropertyPhotoResponse> photos, int totalCount) {

    public static PropertyPhotoListResponse from(final PropertyPhotoListResult result) {
        return new PropertyPhotoListResponse(
                result.photos().stream()
                        .map(PropertyPhotoResponse::from)
                        .toList(),
                result.totalCount()
        );
    }
}
