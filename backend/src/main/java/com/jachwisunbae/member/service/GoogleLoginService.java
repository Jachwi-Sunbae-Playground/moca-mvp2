package com.jachwisunbae.member.service;

import com.jachwisunbae.common.exception.client.InvalidCommandException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.security.IssuedAccessToken;
import com.jachwisunbae.common.security.JwtAccessTokenIssuer;
import com.jachwisunbae.member.client.GoogleIdTokenVerifier;
import com.jachwisunbae.member.client.GoogleOAuthGateway;
import com.jachwisunbae.member.client.GoogleOAuthProperties;
import com.jachwisunbae.member.client.GoogleToken;
import com.jachwisunbae.member.client.VerifiedGoogleProfile;
import com.jachwisunbae.member.domain.Member;
import com.jachwisunbae.member.service.dto.command.GoogleLoginCommand;
import com.jachwisunbae.member.service.dto.result.LoginResult;
import com.jachwisunbae.member.service.dto.result.MemberResult;
import org.springframework.stereotype.Service;

@Service
public class GoogleLoginService {

    private static final String BEARER_TOKEN_TYPE = "Bearer";

    private final GoogleOAuthProperties googleOAuthProperties;
    private final GoogleOAuthGateway googleOAuthGateway;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final MemberAuthenticationService memberAuthenticationService;
    private final JwtAccessTokenIssuer jwtAccessTokenIssuer;

    public GoogleLoginService(
            final GoogleOAuthProperties googleOAuthProperties,
            final GoogleOAuthGateway googleOAuthGateway,
            final GoogleIdTokenVerifier googleIdTokenVerifier,
            final MemberAuthenticationService memberAuthenticationService,
            final JwtAccessTokenIssuer jwtAccessTokenIssuer
    ) {
        this.googleOAuthProperties = googleOAuthProperties;
        this.googleOAuthGateway = googleOAuthGateway;
        this.googleIdTokenVerifier = googleIdTokenVerifier;
        this.memberAuthenticationService = memberAuthenticationService;
        this.jwtAccessTokenIssuer = jwtAccessTokenIssuer;
    }

    public LoginResult login(final GoogleLoginCommand command) {
        validateRedirectUri(command.redirectUri());
        final GoogleToken googleToken = googleOAuthGateway.exchange(
                command.authorizationCode(),
                command.codeVerifier(),
                command.redirectUri()
        );
        final VerifiedGoogleProfile profile = googleIdTokenVerifier.verify(
                googleToken.idToken(),
                command.nonce()
        );
        final Member member = memberAuthenticationService.authenticate(profile);
        final IssuedAccessToken accessToken = jwtAccessTokenIssuer.issue(member.id());

        return new LoginResult(
                accessToken.value(),
                BEARER_TOKEN_TYPE,
                accessToken.expiresInSeconds(),
                MemberResult.from(member)
        );
    }

    private void validateRedirectUri(final String redirectUri) {
        if (!googleOAuthProperties.isAllowedRedirectUri(redirectUri)) {
            throw new InvalidCommandException(ErrorCode.GOOGLE_AUTHORIZATION_CODE_INVALID);
        }
    }
}
