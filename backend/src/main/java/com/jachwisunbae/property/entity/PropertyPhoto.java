package com.jachwisunbae.property.entity;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PropertyPhoto {

    private final Long id;
    private final Long propertyId;
    private final String storageKey;
    private final String contentType;
    private final Long sizeBytes;
    private final LocalDateTime createdAt;

    private PropertyPhoto(final Long id, final Long propertyId, final String storageKey,
                           final String contentType, final Long sizeBytes, final LocalDateTime createdAt) {
        this.id = id;
        this.propertyId = propertyId;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.createdAt = createdAt;
    }

    public static PropertyPhoto create(final Long propertyId, final String storageKey,
                                       final String contentType, final Long sizeBytes,
                                       final LocalDateTime createdAt) {
        return new PropertyPhoto(null, propertyId, storageKey, contentType, sizeBytes, createdAt);
    }

    public static PropertyPhoto reconstruct(final Long id, final Long propertyId, final String storageKey,
                                           final String contentType, final Long sizeBytes,
                                           final LocalDateTime createdAt) {
        return new PropertyPhoto(id, propertyId, storageKey, contentType, sizeBytes, createdAt);
    }
}
