package com.jachwisunbae.member.controller.dto.response;

import com.jachwisunbae.member.service.dto.result.MemberResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원 프로필")
public record MemberResponse(long memberId, String displayName, String email) {

    public static MemberResponse from(final MemberResult result) {
        return new MemberResponse(result.memberId(), result.displayName(), result.email());
    }
}
