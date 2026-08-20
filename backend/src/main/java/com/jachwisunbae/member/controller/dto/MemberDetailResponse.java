package com.jachwisunbae.member.controller.dto;

import com.jachwisunbae.member.entity.Member;

public record MemberDetailResponse(
    long id, String name, String email
) {

    public static MemberDetailResponse from(Member member) {
        return new MemberDetailResponse(member.getId(), member.getName(), member.getEmail());
    }
}
