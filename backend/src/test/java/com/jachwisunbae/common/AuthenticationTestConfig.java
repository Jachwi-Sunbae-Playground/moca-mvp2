package com.jachwisunbae.common;

import com.jachwisunbae.common.exception.client.InvalidCommandException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.exception.server.UpstreamServiceException;
import com.jachwisunbae.member.client.GoogleIdTokenVerifier;
import com.jachwisunbae.member.client.GoogleOAuthGateway;
import com.jachwisunbae.member.client.GoogleToken;
import com.jachwisunbae.member.client.VerifiedGoogleProfile;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class AuthenticationTestConfig {

    private static final String VALID_CODE_PREFIX = "valid-code:";
    private static final String VALID_ID_TOKEN_PREFIX = "valid-id-token:";

    @Bean
    @Primary
    public GoogleOAuthGateway testGoogleOAuthGateway() {
        return (authorizationCode, codeVerifier, redirectUri) -> {
            if ("upstream-failure".equals(authorizationCode)) {
                throw new UpstreamServiceException(ErrorCode.GOOGLE_AUTHENTICATION_FAILED);
            }
            if (!authorizationCode.startsWith(VALID_CODE_PREFIX)) {
                throw new InvalidCommandException(ErrorCode.GOOGLE_AUTHORIZATION_CODE_INVALID);
            }
            return new GoogleToken(VALID_ID_TOKEN_PREFIX + authorizationCode.substring(VALID_CODE_PREFIX.length()));
        };
    }

    @Bean
    @Primary
    public GoogleIdTokenVerifier testGoogleIdTokenVerifier() {
        return (idToken, expectedNonce) -> {
            if (!"valid-nonce".equals(expectedNonce) || !idToken.startsWith(VALID_ID_TOKEN_PREFIX)) {
                throw new InvalidCommandException(ErrorCode.GOOGLE_IDENTITY_INVALID);
            }
            final String subject = idToken.substring(VALID_ID_TOKEN_PREFIX.length());
            if (subject.isBlank() || "invalid-identity".equals(subject)) {
                throw new InvalidCommandException(ErrorCode.GOOGLE_IDENTITY_INVALID);
            }
            return new VerifiedGoogleProfile(subject, subject + "@example.com", "테스트 사용자");
        };
    }
}
