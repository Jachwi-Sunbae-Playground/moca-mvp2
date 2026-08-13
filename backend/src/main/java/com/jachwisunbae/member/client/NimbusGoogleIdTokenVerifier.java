package com.jachwisunbae.member.client;

import com.jachwisunbae.common.exception.client.InvalidCommandException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.exception.server.UpstreamServiceException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

@Component
public class NimbusGoogleIdTokenVerifier implements GoogleIdTokenVerifier {

    private static final int MAXIMUM_SUBJECT_LENGTH = 255;
    private static final int MAXIMUM_EMAIL_LENGTH = 320;
    private static final int MAXIMUM_DISPLAY_NAME_CODE_POINTS = 100;
    private static final String DEFAULT_DISPLAY_NAME = "자취생";

    private final JwtDecoder googleJwtDecoder;

    public NimbusGoogleIdTokenVerifier(
            @Qualifier("googleJwtDecoder") final JwtDecoder googleJwtDecoder
    ) {
        this.googleJwtDecoder = googleJwtDecoder;
    }

    @Override
    public VerifiedGoogleProfile verify(final String idToken, final String expectedNonce) {
        try {
            final Jwt jwt = googleJwtDecoder.decode(idToken);
            validateNonce(jwt.getClaimAsString("nonce"), expectedNonce);

            final String subject = requireText(jwt.getSubject(), MAXIMUM_SUBJECT_LENGTH);
            final String email = requireText(jwt.getClaimAsString("email"), MAXIMUM_EMAIL_LENGTH);
            if (!Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))) {
                throw invalidIdentity();
            }
            final String displayName = normalizeDisplayName(jwt.getClaimAsString("name"));
            return new VerifiedGoogleProfile(subject, email, displayName);
        } catch (InvalidCommandException exception) {
            throw exception;
        } catch (BadJwtException exception) {
            throw new InvalidCommandException(ErrorCode.GOOGLE_IDENTITY_INVALID, exception);
        } catch (JwtException exception) {
            throw new UpstreamServiceException(ErrorCode.GOOGLE_AUTHENTICATION_FAILED, exception);
        }
    }

    private void validateNonce(final String actualNonce, final String expectedNonce) {
        if (actualNonce == null || expectedNonce == null || expectedNonce.isBlank()) {
            throw invalidIdentity();
        }
        final boolean matches = MessageDigest.isEqual(
                actualNonce.getBytes(StandardCharsets.UTF_8),
                expectedNonce.getBytes(StandardCharsets.UTF_8)
        );
        if (!matches) {
            throw invalidIdentity();
        }
    }

    private String requireText(final String value, final int maximumLength) {
        if (value == null || value.isBlank() || value.length() > maximumLength) {
            throw invalidIdentity();
        }
        return value;
    }

    private String normalizeDisplayName(final String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_DISPLAY_NAME;
        }
        final String trimmed = value.trim();
        final int codePointCount = trimmed.codePointCount(0, trimmed.length());
        if (codePointCount <= MAXIMUM_DISPLAY_NAME_CODE_POINTS) {
            return trimmed;
        }
        final int endIndex = trimmed.offsetByCodePoints(0, MAXIMUM_DISPLAY_NAME_CODE_POINTS);
        return trimmed.substring(0, endIndex);
    }

    private InvalidCommandException invalidIdentity() {
        return new InvalidCommandException(ErrorCode.GOOGLE_IDENTITY_INVALID);
    }
}
