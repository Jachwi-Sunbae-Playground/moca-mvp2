package com.jachwisunbae.member.service.dto.command;

public record GoogleLoginCommand(
        String authorizationCode,
        String codeVerifier,
        String nonce,
        String redirectUri
) {
}
