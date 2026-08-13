package com.jachwisunbae.member.client;

public interface GoogleOAuthGateway {

    GoogleToken exchange(
            String authorizationCode,
            String codeVerifier,
            String redirectUri
    );
}
