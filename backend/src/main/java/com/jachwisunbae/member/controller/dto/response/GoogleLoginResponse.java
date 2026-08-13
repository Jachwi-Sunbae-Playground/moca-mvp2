package com.jachwisunbae.member.controller.dto.response;

import com.jachwisunbae.member.service.dto.result.LoginResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "자취선배 Access Token과 로그인 회원")
public record GoogleLoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        MemberResponse member
) {

    public static GoogleLoginResponse from(final LoginResult result) {
        return new GoogleLoginResponse(
                result.accessToken(),
                result.tokenType(),
                result.expiresIn(),
                MemberResponse.from(result.member())
        );
    }
}
