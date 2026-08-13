package com.jachwisunbae.member.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

class GoogleIdentityTokenValidatorTest {

    private static final Instant NOW = Instant.parse("2026-08-10T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final GoogleIdentityTokenValidator VALIDATOR = new GoogleIdentityTokenValidator(
            "google-client-id",
            CLOCK
    );

    @DisplayName("Google issuer, audience와 만료 시각이 유효한 ID Token을 허용한다")
    @Test
    void validIdentityToken() {
        final OAuth2TokenValidatorResult result = VALIDATOR.validate(jwt(
                "https://accounts.google.com",
                List.of("google-client-id"),
                NOW.plusSeconds(3_600)
        ));

        assertThat(result.hasErrors()).isFalse();
    }

    @DisplayName("Google client ID가 audience에 없으면 ID Token을 거부한다")
    @Test
    void invalidAudience() {
        final OAuth2TokenValidatorResult result = VALIDATOR.validate(jwt(
                "https://accounts.google.com",
                List.of("other-client-id"),
                NOW.plusSeconds(3_600)
        ));

        assertThat(result.hasErrors()).isTrue();
    }

    @DisplayName("만료된 Google ID Token을 거부한다")
    @Test
    void expiredIdentityToken() {
        final OAuth2TokenValidatorResult result = VALIDATOR.validate(jwt(
                "https://accounts.google.com",
                List.of("google-client-id"),
                NOW.minusSeconds(120)
        ));

        assertThat(result.hasErrors()).isTrue();
    }

    private Jwt jwt(final String issuer, final List<String> audience, final Instant expiresAt) {
        return new Jwt(
                "id-token",
                expiresAt.minusSeconds(3_600),
                expiresAt,
                Map.of("alg", "RS256"),
                Map.of(
                        "iss", issuer,
                        "aud", audience,
                        "sub", "google-subject"
                )
        );
    }
}
