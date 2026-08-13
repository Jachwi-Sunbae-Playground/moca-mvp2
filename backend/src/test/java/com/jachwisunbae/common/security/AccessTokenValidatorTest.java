package com.jachwisunbae.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

class AccessTokenValidatorTest {

    private static final Instant NOW = Instant.parse("2026-08-10T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final JwtProperties PROPERTIES = new JwtProperties(
            Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes()),
            "jachwi-sunbae",
            "jachwi-sunbae-api",
            Duration.ofHours(12),
            Duration.ofSeconds(60)
    );
    private static final AccessTokenValidator VALIDATOR = new AccessTokenValidator(PROPERTIES, CLOCK);

    @DisplayName("발급자, 대상, 회원 subject와 시간이 올바른 Access Token을 허용한다")
    @Test
    void validAccessToken() {
        final OAuth2TokenValidatorResult result = VALIDATOR.validate(jwt(
                "jachwi-sunbae",
                List.of("jachwi-sunbae-api"),
                "1",
                NOW.minusSeconds(10),
                NOW.plusSeconds(3_600)
        ));

        assertThat(result.hasErrors()).isFalse();
    }

    @DisplayName("발급자가 다른 Access Token을 거부한다")
    @Test
    void invalidIssuer() {
        final OAuth2TokenValidatorResult result = VALIDATOR.validate(jwt(
                "other-issuer",
                List.of("jachwi-sunbae-api"),
                "1",
                NOW.minusSeconds(10),
                NOW.plusSeconds(3_600)
        ));

        assertThat(result.hasErrors()).isTrue();
    }

    @DisplayName("대상이 다른 Access Token을 거부한다")
    @Test
    void invalidAudience() {
        final OAuth2TokenValidatorResult result = VALIDATOR.validate(jwt(
                "jachwi-sunbae",
                List.of("other-api"),
                "1",
                NOW.minusSeconds(10),
                NOW.plusSeconds(3_600)
        ));

        assertThat(result.hasErrors()).isTrue();
    }

    @DisplayName("만료된 Access Token을 전용 오류로 분류한다")
    @Test
    void expiredAccessToken() {
        final OAuth2TokenValidatorResult result = VALIDATOR.validate(jwt(
                "jachwi-sunbae",
                List.of("jachwi-sunbae-api"),
                "1",
                NOW.minusSeconds(7_200),
                NOW.minusSeconds(120)
        ));

        assertThat(result.getErrors())
                .extracting(error -> error.getErrorCode())
                .containsExactly(AccessTokenValidator.EXPIRED_ERROR_CODE);
    }

    @DisplayName("양수가 아닌 회원 subject의 Access Token을 거부한다")
    @Test
    void invalidSubject() {
        final OAuth2TokenValidatorResult result = VALIDATOR.validate(jwt(
                "jachwi-sunbae",
                List.of("jachwi-sunbae-api"),
                "0",
                NOW.minusSeconds(10),
                NOW.plusSeconds(3_600)
        ));

        assertThat(result.hasErrors()).isTrue();
    }

    private Jwt jwt(
            final String issuer,
            final List<String> audience,
            final String subject,
            final Instant issuedAt,
            final Instant expiresAt
    ) {
        return new Jwt(
                "access-token",
                issuedAt,
                expiresAt,
                Map.of("alg", "HS256"),
                Map.of(
                        "iss", issuer,
                        "aud", audience,
                        "sub", subject,
                        "iat", issuedAt,
                        "exp", expiresAt,
                        "tokenType", "ACCESS"
                )
        );
    }
}
