package com.jachwisunbae.member.service;

import com.jachwisunbae.member.entity.Member;

public record MemberProfile(Member member, boolean passwordProtected) {
}
