package com.jachwisunbae.property.service.dto.result;

import java.util.List;

public record PropertyPhotoListResult(List<PropertyPhotoResult> photos, int totalCount) {

    public PropertyPhotoListResult {
        photos = List.copyOf(photos);
    }
}
