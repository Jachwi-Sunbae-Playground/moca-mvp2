package com.jachwisunbae.auth.controller.dto;

import com.jachwisunbae.auth.provider.OAuthLoginCommand;

import jakarta.validation.constraints.NotBlank;

public record OAuthLoginRequest(
        @NotBlank String authorizationCode,
        @NotBlank String codeVerifier,
        @NotBlank String nonce,
        @NotBlank String redirectUri) {

    public OAuthLoginCommand toCommand() {
        return new OAuthLoginCommand(authorizationCode, codeVerifier, nonce, redirectUri);
    }
}
