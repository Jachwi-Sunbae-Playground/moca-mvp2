package com.jachwisunbae.common.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

    @DisplayName("Base64 디코딩 후 32바이트보다 짧은 JWT 비밀키를 거부한다")
    @Test
    void rejectWeakSecret() {
        final String weakSecret = Base64.getEncoder().encodeToString("too-short-secret".getBytes());

        assertThatThrownBy(() -> new JwtProperties(
                weakSecret,
                "jachwi-sunbae",
                "jachwi-sunbae-api",
                Duration.ofHours(12),
                Duration.ofSeconds(60)
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("Base64 형식이 아닌 JWT 비밀키를 거부한다")
    @Test
    void rejectMalformedSecret() {
        assertThatThrownBy(() -> new JwtProperties(
                "not-base64!",
                "jachwi-sunbae",
                "jachwi-sunbae-api",
                Duration.ofHours(12),
                Duration.ofSeconds(60)
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
