package com.jachwisunbae.property.controller.dto.response;

import com.jachwisunbae.property.entity.PropertyPhoto;
import java.time.LocalDateTime;

public record PropertyPhotoResponse(Long id, Long propertyId, String url, String contentType,
                                    Long sizeBytes, boolean representative, LocalDateTime createdAt) {
    public static PropertyPhotoResponse from(final PropertyPhoto photo, final boolean representative) {
        return new PropertyPhotoResponse(photo.getId(), photo.getPropertyId(),
                "/api/properties/" + photo.getPropertyId() + "/photos/" + photo.getId(),
                photo.getContentType(), photo.getSizeBytes(), representative, photo.getCreatedAt());
    }
}
