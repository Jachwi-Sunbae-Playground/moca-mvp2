package com.jachwisunbae.auth.provider;

public record OAuthProfile(
        String subject,
        String email,
        String name) {
}
