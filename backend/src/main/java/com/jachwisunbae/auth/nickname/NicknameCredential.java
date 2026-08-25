package com.jachwisunbae.auth.nickname;

import java.time.LocalDateTime;

public record NicknameCredential(
        Long memberId,
        String nickname,
        String nicknameKey,
        String passwordHash,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public boolean passwordProtected() {
        return passwordHash != null;
    }
}
