package com.jachwisunbae.auth.provider;

public interface OAuthProvider {

    OAuthProviderType type();

    OAuthProfile authenticate(OAuthLoginCommand command);
}
