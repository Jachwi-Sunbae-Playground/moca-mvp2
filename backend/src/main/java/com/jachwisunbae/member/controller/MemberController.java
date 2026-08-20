package com.jachwisunbae.member.controller;

import com.jachwisunbae.auth.web.AuthenticatedMemberId;
import com.jachwisunbae.common.web.ApiResponse;
import com.jachwisunbae.member.controller.dto.MemberDetailResponse;
import com.jachwisunbae.member.service.MemberService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    public ApiResponse<MemberDetailResponse> get(
        @AuthenticatedMemberId final long memberId
    ){
        return ApiResponse.of(MemberDetailResponse.from(memberService.findById(memberId)));
    }

}
