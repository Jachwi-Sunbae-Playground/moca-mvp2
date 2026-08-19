package com.jachwisunbae.auth.google;

import com.jachwisunbae.auth.provider.OAuthLoginCommand;
import com.jachwisunbae.auth.provider.OAuthProfile;
import com.jachwisunbae.auth.provider.OAuthProvider;
import com.jachwisunbae.auth.provider.OAuthProviderType;
import org.springframework.stereotype.Component;

@Component
public class GoogleOAuthProvider implements OAuthProvider {

    private final GoogleOAuthClient client;
    private final GoogleIdentityVerifier verifier;

    public GoogleOAuthProvider(GoogleOAuthClient client, GoogleIdentityVerifier verifier) {
        this.client = client;
        this.verifier = verifier;
    }

    @Override
    public OAuthProviderType type() {
        return OAuthProviderType.GOOGLE;
    }

    @Override
    public OAuthProfile authenticate(OAuthLoginCommand command) {
        GoogleTokenResponse tokens = client.exchange(
                command.authorizationCode(),
                command.codeVerifier(),
                command.redirectUri());
        GoogleProfile profile = verifier.verify(tokens.idToken(), command.nonce());
        return new OAuthProfile(profile.subject(), profile.email(), profile.name());
    }
}
