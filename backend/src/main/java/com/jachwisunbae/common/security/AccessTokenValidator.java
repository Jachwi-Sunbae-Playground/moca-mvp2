package com.jachwisunbae.common.security;

import java.time.Clock;
import java.time.Instant;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class AccessTokenValidator implements OAuth2TokenValidator<Jwt> {

    public static final String EXPIRED_ERROR_CODE = "access_token_expired";
    private static final OAuth2Error INVALID_TOKEN = new OAuth2Error(
            "invalid_token",
            "Access Token claim is invalid",
            null
    );
    private static final OAuth2Error EXPIRED_TOKEN = new OAuth2Error(
            EXPIRED_ERROR_CODE,
            "Access Token is expired",
            null
    );

    private final JwtProperties properties;
    private final Clock clock;

    public AccessTokenValidator(final JwtProperties properties, final Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public OAuth2TokenValidatorResult validate(final Jwt jwt) {
        final Instant now = clock.instant();
        final Instant expiresAt = jwt.getExpiresAt();
        if (expiresAt == null) {
            return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
        }
        if (now.minus(properties.clockSkew()).isAfter(expiresAt)) {
            return OAuth2TokenValidatorResult.failure(EXPIRED_TOKEN);
        }
        if (jwt.getNotBefore() != null && now.plus(properties.clockSkew()).isBefore(jwt.getNotBefore())) {
            return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
        }
        if (jwt.getIssuedAt() == null || now.plus(properties.clockSkew()).isBefore(jwt.getIssuedAt())) {
            return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
        }
        if (!properties.issuer().equals(jwt.getClaimAsString("iss"))) {
            return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
        }
        if (!jwt.getAudience().contains(properties.audience())) {
            return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
        }
        if (!"ACCESS".equals(jwt.getClaimAsString("tokenType"))) {
            return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
        }
        if (!hasPositiveSubject(jwt.getSubject())) {
            return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
        }
        return OAuth2TokenValidatorResult.success();
    }

    private boolean hasPositiveSubject(final String subject) {
        try {
            return subject != null && Long.parseLong(subject) > 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}
