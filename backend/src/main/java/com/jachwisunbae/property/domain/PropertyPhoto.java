package com.jachwisunbae.property.domain;

import java.time.Instant;
import java.util.Set;

public record PropertyPhoto(
        long id,
        long propertyId,
        long memberId,
        String storageKey,
        String contentType,
        long sizeBytes,
        String checksumSha256,
        Instant createdAt
) {

    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    public PropertyPhoto {
        if (id < 0) {
            throw new IllegalArgumentException("사진 식별자는 음수일 수 없습니다.");
        }
        if (propertyId <= 0 || memberId <= 0) {
            throw new IllegalArgumentException("사진의 매물과 회원 식별자는 양수여야 합니다.");
        }
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("사진 저장 키는 비어 있을 수 없습니다.");
        }
        if (storageKey.length() > 512) {
            throw new IllegalArgumentException("사진 저장 키가 너무 깁니다.");
        }
        if (!SUPPORTED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("지원하지 않는 사진 형식입니다.");
        }
        if (sizeBytes <= 0 || sizeBytes > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("사진 크기가 허용 범위를 벗어났습니다.");
        }
        if (checksumSha256 == null || !checksumSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("사진 체크섬이 올바르지 않습니다.");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("사진 생성 시각은 필수입니다.");
        }
    }

    public PropertyPhoto withId(final long generatedId) {
        return new PropertyPhoto(
                generatedId,
                propertyId,
                memberId,
                storageKey,
                contentType,
                sizeBytes,
                checksumSha256,
                createdAt
        );
    }
}
