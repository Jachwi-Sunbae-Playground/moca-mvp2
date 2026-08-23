package com.jachwisunbae.property.controller.dto.response;

import com.jachwisunbae.property.entity.PropertyPhoto;

import java.time.Instant;
import java.time.ZoneOffset;

public record PropertyDetailPhoto(Long id, String url, String contentType, Long sizeBytes,
                                  boolean representative, Instant createdAt) {
    public static PropertyDetailPhoto from(final PropertyPhoto photo, final boolean representative) {
        return new PropertyDetailPhoto(photo.getId(),
                "/api/properties/" + photo.getPropertyId() + "/photos/" + photo.getId(), photo.getContentType(),
                photo.getSizeBytes(), representative, photo.getCreatedAt().toInstant(ZoneOffset.UTC));
    }
}
