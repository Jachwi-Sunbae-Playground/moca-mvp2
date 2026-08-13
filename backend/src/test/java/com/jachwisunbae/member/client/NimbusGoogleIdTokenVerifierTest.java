package com.jachwisunbae.member.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jachwisunbae.common.exception.client.InvalidCommandException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class NimbusGoogleIdTokenVerifierTest {

    @DisplayName("검증된 Google claim을 회원 프로필로 변환한다")
    @Test
    void verifyGoogleProfile() {
        final JwtDecoder decoder = token -> jwt(Map.of(
                "sub", "google-subject",
                "email", "member@example.com",
                "email_verified", true,
                "name", "  표시 이름  ",
                "nonce", "expected-nonce"
        ));
        final NimbusGoogleIdTokenVerifier verifier = new NimbusGoogleIdTokenVerifier(decoder);

        final VerifiedGoogleProfile profile = verifier.verify("id-token", "expected-nonce");

        assertThat(profile).isEqualTo(new VerifiedGoogleProfile(
                "google-subject",
                "member@example.com",
                "표시 이름"
        ));
    }

    @DisplayName("Google 표시 이름이 없으면 기본 이름을 사용한다")
    @Test
    void useDefaultDisplayName() {
        final JwtDecoder decoder = token -> jwt(Map.of(
                "sub", "google-subject",
                "email", "member@example.com",
                "email_verified", true,
                "nonce", "expected-nonce"
        ));
        final NimbusGoogleIdTokenVerifier verifier = new NimbusGoogleIdTokenVerifier(decoder);

        final VerifiedGoogleProfile profile = verifier.verify("id-token", "expected-nonce");

        assertThat(profile.displayName()).isEqualTo("자취생");
    }

    @DisplayName("Google ID Token nonce가 요청 nonce와 다르면 신원 오류로 거부한다")
    @Test
    void rejectMismatchedNonce() {
        final JwtDecoder decoder = token -> jwt(Map.of(
                "sub", "google-subject",
                "email", "member@example.com",
                "email_verified", true,
                "nonce", "other-nonce"
        ));
        final NimbusGoogleIdTokenVerifier verifier = new NimbusGoogleIdTokenVerifier(decoder);

        assertThatThrownBy(() -> verifier.verify("id-token", "expected-nonce"))
                .isInstanceOfSatisfying(InvalidCommandException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.GOOGLE_IDENTITY_INVALID)
                );
    }

    @DisplayName("검증된 email claim이 없으면 신원 오류로 거부한다")
    @Test
    void rejectUnverifiedEmail() {
        final JwtDecoder decoder = token -> jwt(Map.of(
                "sub", "google-subject",
                "email", "member@example.com",
                "email_verified", false,
                "nonce", "expected-nonce"
        ));
        final NimbusGoogleIdTokenVerifier verifier = new NimbusGoogleIdTokenVerifier(decoder);

        assertThatThrownBy(() -> verifier.verify("id-token", "expected-nonce"))
                .isInstanceOfSatisfying(InvalidCommandException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.GOOGLE_IDENTITY_INVALID)
                );
    }

    private Jwt jwt(final Map<String, Object> claims) {
        return new Jwt(
                "id-token",
                Instant.parse("2026-08-10T00:00:00Z"),
                Instant.parse("2026-08-10T01:00:00Z"),
                Map.of("alg", "RS256"),
                claims
        );
    }
}
