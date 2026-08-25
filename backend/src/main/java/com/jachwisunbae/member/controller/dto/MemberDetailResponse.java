package com.jachwisunbae.member.controller.dto;

import com.jachwisunbae.member.service.MemberProfile;

public record MemberDetailResponse(
    long id, String name, boolean passwordProtected
) {

    public static MemberDetailResponse from(MemberProfile profile) {
        return new MemberDetailResponse(profile.member().getId(), profile.member().getName(),
                profile.passwordProtected());
    }
}
