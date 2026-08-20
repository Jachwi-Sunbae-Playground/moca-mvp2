package com.jachwisunbae.property.entity;

import lombok.Getter;
import com.jachwisunbae.common.exception.DomainErrorCode;
import com.jachwisunbae.common.validation.DomainPreconditions;

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
        return new MainPropertyPhoto(null, validateId(propertyId), validateId(propertyPhotosId));
    }

    public static MainPropertyPhoto reconstruct(final Long id, final Long propertyId, final Long propertyPhotosId) {
        return new MainPropertyPhoto(id, validateId(propertyId), validateId(propertyPhotosId));
    }

    private static Long validateId(final Long id) {
        return DomainPreconditions.requireNonNull(id, DomainErrorCode.PROPERTY_INPUT_INVALID,
                "대표 사진과 매물 ID는 필수입니다.");
    }
}
