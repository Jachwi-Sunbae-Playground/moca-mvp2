package com.jachwisunbae.common.security;

public record IssuedAccessToken(String value, long expiresInSeconds) {
}
