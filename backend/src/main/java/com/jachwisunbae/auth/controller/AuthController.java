package com.jachwisunbae.auth.controller;

import com.jachwisunbae.auth.controller.dto.LoginResponse;
import com.jachwisunbae.auth.controller.dto.OAuthLoginRequest;
import com.jachwisunbae.auth.provider.OAuthProviderType;
import com.jachwisunbae.auth.service.AuthService;
import com.jachwisunbae.common.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "OAuth 로그인과 Access Token 발급 API")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/{provider}")
    @Operation(summary = "OAuth 로그인", description = "인가 코드를 검증하고 회원 생성 또는 조회 후 Access Token을 발급합니다.")
    public ApiResponse<LoginResponse> login(
            @PathVariable String provider,
            @Valid @RequestBody OAuthLoginRequest request) {
        return ApiResponse.of(service.login(OAuthProviderType.from(provider), request));
    }

}
