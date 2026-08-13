package com.jachwisunbae.member.controller;

import com.jachwisunbae.common.response.ApiResponse;
import com.jachwisunbae.common.response.ErrorResponse;
import com.jachwisunbae.member.controller.dto.request.GoogleLoginRequest;
import com.jachwisunbae.member.controller.dto.response.GoogleLoginResponse;
import com.jachwisunbae.member.service.GoogleLoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final GoogleLoginService googleLoginService;

    public AuthController(final GoogleLoginService googleLoginService) {
        this.googleLoginService = googleLoginService;
    }

    @Operation(
            summary = "Google 코드 교환 로그인",
            description = "Google authorization code를 검증하고 자취선배 JWT Access Token을 발급한다. 인증은 필요하지 않다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청·Google 코드·Google 신원 검증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502",
                    description = "Google 인증 서비스 통신 또는 응답 처리 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<GoogleLoginResponse>> login(
            @Valid @RequestBody final GoogleLoginRequest request
    ) {
        final GoogleLoginResponse response = GoogleLoginResponse.from(
                googleLoginService.login(request.toCommand())
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
