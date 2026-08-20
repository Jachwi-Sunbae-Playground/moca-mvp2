package com.jachwisunbae.member.controller;

import com.jachwisunbae.auth.web.AuthenticatedMemberId;
import com.jachwisunbae.common.web.ApiResponse;
import com.jachwisunbae.member.controller.dto.MemberDetailResponse;
import com.jachwisunbae.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@Tag(name = "Members", description = "현재 로그인 회원 조회 API")
@SecurityRequirement(name = "bearerAuth")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/me")
    @Operation(summary = "현재 회원 정보 조회", description = "Access Token의 회원 ID로 이름과 이메일을 조회합니다.")
    public ApiResponse<MemberDetailResponse> get(
        @AuthenticatedMemberId final Long memberId
    ){
        return ApiResponse.of(MemberDetailResponse.from(memberService.findById(memberId)));
    }

}
