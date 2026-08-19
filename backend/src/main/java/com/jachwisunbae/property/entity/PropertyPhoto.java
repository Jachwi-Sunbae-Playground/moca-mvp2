package com.jachwisunbae.property.entity;

import lombok.Getter;
import com.jachwisunbae.common.exception.DomainErrorCode;
import com.jachwisunbae.common.validation.DomainPreconditions;

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
        return new PropertyPhoto(null, validateId(propertyId), validateText(storageKey), validateContentType(contentType),
                validateSize(sizeBytes), requireCreatedAt(createdAt));
    }

    public static PropertyPhoto reconstruct(final Long id, final Long propertyId, final String storageKey,
                                           final String contentType, final Long sizeBytes,
                                           final LocalDateTime createdAt) {
        return new PropertyPhoto(id, validateId(propertyId), validateText(storageKey), validateContentType(contentType),
                validateSize(sizeBytes), requireCreatedAt(createdAt));
    }

    private static Long validateId(final Long id) {
        return DomainPreconditions.requireNonNull(id, DomainErrorCode.PROPERTY_INPUT_INVALID,
                "사진의 매물 ID는 필수입니다.");
    }

    private static String validateText(final String value) {
        return DomainPreconditions.requireNonBlank(value, DomainErrorCode.PROPERTY_INPUT_INVALID,
                "사진 저장 키는 필수입니다.");
    }

    private static String validateContentType(final String value) {
        String type = DomainPreconditions.requireNonBlank(value, DomainErrorCode.PROPERTY_INPUT_INVALID,
                "사진 콘텐츠 타입은 필수입니다.").toLowerCase();
        DomainPreconditions.require(type.equals("image/jpeg") || type.equals("image/png")
                        || type.equals("image/webp") || type.equals("image/heic") || type.equals("image/heif"),
                DomainErrorCode.PROPERTY_INPUT_INVALID, "JPG, JPEG, PNG, WebP, Heic만 허용됩니다.");
        return type;
    }

    private static Long validateSize(final Long sizeBytes) {
        return DomainPreconditions.requireAtMost(
                DomainPreconditions.requireNonNegative(sizeBytes, DomainErrorCode.PROPERTY_INPUT_INVALID,
                        "사진 크기는 0 이상의 값이어야 합니다."),
                5L * 1024 * 1024, DomainErrorCode.PROPERTY_INPUT_INVALID,
                "사진 크기는 5MiB 이하여야 합니다.");
    }

    private static java.time.LocalDateTime requireCreatedAt(final java.time.LocalDateTime createdAt) {
        return DomainPreconditions.requireNonNull(createdAt, DomainErrorCode.PROPERTY_INPUT_INVALID,
                "사진 업로드 시각은 필수입니다.");
    }
}
