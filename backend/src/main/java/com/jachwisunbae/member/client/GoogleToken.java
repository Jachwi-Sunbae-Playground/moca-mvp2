package com.jachwisunbae.member.client;

public record GoogleToken(String idToken) {

    public GoogleToken {
        if (idToken == null || idToken.isBlank()) {
            throw new IllegalArgumentException("Google ID Token은 비어 있을 수 없습니다.");
        }
    }
}
