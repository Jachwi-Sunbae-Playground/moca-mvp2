package com.jachwisunbae.property.controller.dto.response;

import com.jachwisunbae.property.service.dto.result.PropertyPhotoResult;
import java.time.Instant;

public record PropertyPhotoResponse(
        long photoId,
        String contentUrl,
        String contentType,
        long sizeBytes,
        Instant createdAt
) {

    public static PropertyPhotoResponse from(final PropertyPhotoResult result) {
        return new PropertyPhotoResponse(
                result.photoId(),
                "/api/properties/%d/photos/%d/content".formatted(result.propertyId(), result.photoId()),
                result.contentType(),
                result.sizeBytes(),
                result.createdAt()
        );
    }
}
