package com.jachwisunbae.common.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("security.jwt")
public record JwtProperties(
        @NotBlank String secretBase64,
        @NotBlank String issuer,
        @NotBlank String audience,
        @NotNull Duration accessTokenTtl,
        @NotNull Duration clockSkew
) {

    private static final int MINIMUM_SECRET_BYTES = 32;

    public JwtProperties {
        if (accessTokenTtl != null && (accessTokenTtl.isZero() || accessTokenTtl.isNegative())) {
            throw new IllegalArgumentException("Access Token TTL은 양수여야 합니다.");
        }
        if (clockSkew != null && clockSkew.isNegative()) {
            throw new IllegalArgumentException("JWT clock skew는 음수일 수 없습니다.");
        }
        if (secretBase64 != null && !secretBase64.isBlank()) {
            validateSecret(secretBase64);
        }
    }

    public SecretKey secretKey() {
        return new SecretKeySpec(decodeSecret(secretBase64), "HmacSHA256");
    }

    private static void validateSecret(final String secretBase64) {
        final byte[] decoded = decodeSecret(secretBase64);
        if (decoded.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalArgumentException("JWT 비밀키는 Base64 디코딩 후 32바이트 이상이어야 합니다.");
        }
    }

    private static byte[] decodeSecret(final String secretBase64) {
        try {
            return Base64.getDecoder().decode(secretBase64);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("JWT 비밀키는 올바른 Base64 형식이어야 합니다.", exception);
        }
    }
}
