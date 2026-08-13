package com.jachwisunbae.property.service.dto.result;

import com.jachwisunbae.property.domain.PropertyPhoto;
import java.time.Instant;

public record PropertyPhotoResult(
        long propertyId,
        long photoId,
        String contentType,
        long sizeBytes,
        Instant createdAt
) {

    public static PropertyPhotoResult from(final PropertyPhoto photo) {
        return new PropertyPhotoResult(
                photo.propertyId(),
                photo.id(),
                photo.contentType(),
                photo.sizeBytes(),
                photo.createdAt()
        );
    }
}
