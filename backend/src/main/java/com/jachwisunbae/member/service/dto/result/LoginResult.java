package com.jachwisunbae.member.service.dto.result;

public record LoginResult(
        String accessToken,
        String tokenType,
        long expiresIn,
        MemberResult member
) {
}
