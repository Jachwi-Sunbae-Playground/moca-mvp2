package com.jachwisunbae.member.domain;

import java.time.Instant;

public record Member(
        long id,
        OAuthProvider oauthProvider,
        String oauthSubject,
        String email,
        String displayName,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt
) {

    public Member {
        if (id <= 0) {
            throw new IllegalArgumentException("회원 ID는 양수여야 합니다.");
        }
        if (oauthProvider == null || oauthSubject == null || oauthSubject.isBlank()) {
            throw new IllegalArgumentException("OAuth 사용자 식별정보가 필요합니다.");
        }
        if (email == null || email.isBlank() || displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("회원 프로필이 필요합니다.");
        }
        if (lastLoginAt == null || createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("회원 시각 정보가 필요합니다.");
        }
    }
}
