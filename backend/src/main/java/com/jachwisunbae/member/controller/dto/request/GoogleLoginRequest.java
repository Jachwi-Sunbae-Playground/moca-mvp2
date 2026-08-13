package com.jachwisunbae.member.controller.dto.request;

import com.jachwisunbae.member.service.dto.command.GoogleLoginCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Google authorization code 교환 요청")
public record GoogleLoginRequest(
        @Schema(description = "Google이 발급한 일회용 authorization code")
        @NotBlank(message = "Google authorization code는 필수입니다.")
        @Size(max = 2048, message = "Google authorization code는 2,048자 이하여야 합니다.")
        String authorizationCode,

        @Schema(description = "PKCE code verifier")
        @NotBlank(message = "PKCE code verifier는 필수입니다.")
        @Size(min = 43, max = 128, message = "PKCE code verifier는 43자 이상 128자 이하여야 합니다.")
        @Pattern(
                regexp = "[A-Za-z0-9\\-._~]+",
                message = "PKCE code verifier 형식이 올바르지 않습니다."
        )
        String codeVerifier,

        @Schema(description = "로그인 시작 시 생성해 Google 인증 요청에 사용한 OIDC nonce")
        @NotBlank(message = "OIDC nonce는 필수입니다.")
        @Size(max = 255, message = "OIDC nonce는 255자 이하여야 합니다.")
        String nonce,

        @Schema(description = "Google OAuth Client에 등록된 callback URI")
        @NotBlank(message = "redirect URI는 필수입니다.")
        @Size(max = 2048, message = "redirect URI는 2,048자 이하여야 합니다.")
        String redirectUri
) {

    public GoogleLoginCommand toCommand() {
        return new GoogleLoginCommand(authorizationCode, codeVerifier, nonce, redirectUri);
    }
}
