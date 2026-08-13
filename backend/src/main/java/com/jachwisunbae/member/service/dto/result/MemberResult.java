package com.jachwisunbae.member.service.dto.result;

import com.jachwisunbae.member.domain.Member;

public record MemberResult(long memberId, String displayName, String email) {

    public static MemberResult from(final Member member) {
        return new MemberResult(member.id(), member.displayName(), member.email());
    }
}
