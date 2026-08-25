package com.jachwisunbae.auth.controller.dto;

public record LoginMemberResponse(Long memberId, String name, boolean passwordProtected) {
}
