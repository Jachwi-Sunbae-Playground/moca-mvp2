package com.jachwisunbae.property.controller.dto.response;

import com.jachwisunbae.property.entity.PropertyPhoto;

import java.time.LocalDateTime;

public record PropertyDetailPhoto(Long id, String url, String contentType, Long sizeBytes,
                                  boolean representative, LocalDateTime createdAt) {
    public static PropertyDetailPhoto from(final PropertyPhoto photo) {
        return new PropertyDetailPhoto(photo.getId(), photo.getStorageKey(), photo.getContentType(),
                photo.getSizeBytes(), false, photo.getCreatedAt());
    }
}
