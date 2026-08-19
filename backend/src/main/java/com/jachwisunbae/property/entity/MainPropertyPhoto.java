package com.jachwisunbae.property.entity;

import lombok.Getter;

@Getter
public class MainPropertyPhoto {

    private final Long id;
    private final Long propertyId;
    private final Long propertyPhotosId;

    private MainPropertyPhoto(final Long id, final Long propertyId, final Long propertyPhotosId) {
        this.id = id;
        this.propertyId = propertyId;
        this.propertyPhotosId = propertyPhotosId;
    }

    public static MainPropertyPhoto create(final Long propertyId, final Long propertyPhotosId) {
        return new MainPropertyPhoto(null, propertyId, propertyPhotosId);
    }

    public static MainPropertyPhoto reconstruct(final Long id, final Long propertyId, final Long propertyPhotosId) {
        return new MainPropertyPhoto(id, propertyId, propertyPhotosId);
    }
}
