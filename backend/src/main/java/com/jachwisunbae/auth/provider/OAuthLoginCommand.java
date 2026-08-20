package com.jachwisunbae.auth.provider;

public record OAuthLoginCommand(
        String authorizationCode,
        String codeVerifier,
        String nonce,
        String redirectUri) {
}
