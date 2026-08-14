package com.jachwisunbae.property.storage;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "photo.storage")
public record PhotoStorageProperties(
        @NotBlank String region,
        @NotBlank String bucket,
        String keyPrefix,
        String endpoint,
        String accessKey,
        String secretKey
) {

    /**
     * {@code endpoint}, {@code accessKey}, {@code secretKey}는 정적 자격증명으로 접속하는 환경에서만 사용한다.
     * 운영은 인스턴스 role로 접속하므로 비어 있으며, 필요 여부는 {@link PhotoStorageConfiguration}이 판단한다.
     */
    public String objectKey(final String storageKey) {
        if (keyPrefix == null || keyPrefix.isBlank()) {
            return storageKey;
        }
        if (keyPrefix.endsWith("/")) {
            return keyPrefix + storageKey;
        }
        return keyPrefix + "/" + storageKey;
    }
}
