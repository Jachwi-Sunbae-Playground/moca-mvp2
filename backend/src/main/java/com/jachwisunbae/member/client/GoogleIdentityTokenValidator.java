package com.jachwisunbae.member.client;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class GoogleIdentityTokenValidator implements OAuth2TokenValidator<Jwt> {

    private static final Set<String> ALLOWED_ISSUERS = Set.of(
            "accounts.google.com",
            "https://accounts.google.com"
    );
    private static final Duration CLOCK_SKEW = Duration.ofSeconds(60);
    private static final OAuth2Error INVALID_IDENTITY = new OAuth2Error(
            "invalid_token",
            "Google ID Token claim is invalid",
            null
    );

    private final String clientId;
    private final Clock clock;

    public GoogleIdentityTokenValidator(final String clientId, final Clock clock) {
        this.clientId = clientId;
        this.clock = clock;
    }

    @Override
    public OAuth2TokenValidatorResult validate(final Jwt jwt) {
        final Instant now = clock.instant();
        final String issuer = jwt.getClaimAsString("iss");
        if (!ALLOWED_ISSUERS.contains(issuer)) {
            return OAuth2TokenValidatorResult.failure(INVALID_IDENTITY);
        }
        if (!jwt.getAudience().contains(clientId)) {
            return OAuth2TokenValidatorResult.failure(INVALID_IDENTITY);
        }
        if (jwt.getExpiresAt() == null || now.minus(CLOCK_SKEW).isAfter(jwt.getExpiresAt())) {
            return OAuth2TokenValidatorResult.failure(INVALID_IDENTITY);
        }
        if (jwt.getNotBefore() != null && now.plus(CLOCK_SKEW).isBefore(jwt.getNotBefore())) {
            return OAuth2TokenValidatorResult.failure(INVALID_IDENTITY);
        }
        if (jwt.getIssuedAt() == null || now.plus(CLOCK_SKEW).isBefore(jwt.getIssuedAt())) {
            return OAuth2TokenValidatorResult.failure(INVALID_IDENTITY);
        }
        return OAuth2TokenValidatorResult.success();
    }
}
