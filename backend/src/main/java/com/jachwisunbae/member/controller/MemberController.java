package com.jachwisunbae.member.controller;

import com.jachwisunbae.common.config.OpenApiConfig;
import com.jachwisunbae.common.resolver.AuthenticatedMemberId;
import com.jachwisunbae.common.response.ApiResponse;
import com.jachwisunbae.common.response.ErrorResponse;
import com.jachwisunbae.member.controller.dto.response.MemberResponse;
import com.jachwisunbae.member.service.MemberQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberQueryService memberQueryService;

    public MemberController(final MemberQueryService memberQueryService) {
        this.memberQueryService = memberQueryService;
    }

    @Operation(
            summary = "현재 사용자 조회",
            description = "검증된 자취선배 JWT Access Token의 현재 회원 프로필을 조회한다.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "현재 회원 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 누락·만료·유효하지 않은 Access Token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponse>> getMe(
            @AuthenticatedMemberId final long memberId
    ) {
        final MemberResponse response = MemberResponse.from(memberQueryService.getMe(memberId));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
