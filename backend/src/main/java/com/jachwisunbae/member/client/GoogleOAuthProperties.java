package com.jachwisunbae.member.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("google.oauth")
public record GoogleOAuthProperties(
        @NotBlank String clientId,
        @NotBlank String clientSecret,
        @NotBlank String tokenUri,
        @NotBlank String jwkSetUri,
        @NotEmpty List<@NotBlank String> allowedRedirectUris
) {

    public GoogleOAuthProperties {
        allowedRedirectUris = List.copyOf(allowedRedirectUris);
    }

    public boolean isAllowedRedirectUri(final String redirectUri) {
        return allowedRedirectUris.contains(redirectUri);
    }
}
