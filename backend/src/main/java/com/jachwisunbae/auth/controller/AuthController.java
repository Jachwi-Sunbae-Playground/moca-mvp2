package com.jachwisunbae.auth.controller;

import com.jachwisunbae.auth.controller.dto.LoginResponse;
import com.jachwisunbae.auth.controller.dto.OAuthLoginRequest;
import com.jachwisunbae.auth.provider.OAuthProviderType;
import com.jachwisunbae.auth.service.AuthService;
import com.jachwisunbae.common.web.ApiResponse;
import com.jachwisunbae.common.exception.BusinessException;
import com.jachwisunbae.common.exception.DomainErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "OAuth 로그인과 Access Token 발급 API")
public class AuthController {

    private final AuthService service;
    private final String authMode;

    public AuthController(AuthService service, @Value("${auth.mode:demo}") String authMode) {
        this.service = service;
        this.authMode = authMode;
    }

    @PostMapping("/demo")
    @Operation(summary = "데모 로그인", description = "demo 모드에서 외부 계정 없이 고정 데모 회원으로 로그인합니다.")
    public ApiResponse<LoginResponse> loginDemo() {
        if (!"demo".equalsIgnoreCase(authMode)) {
            throw new BusinessException(DomainErrorCode.DEMO_AUTH_DISABLED,
                    "현재 실행 모드에서는 데모 로그인을 사용할 수 없습니다.");
        }
        return ApiResponse.of(service.loginDemo());
    }

    @PostMapping("/{provider}")
    @Operation(summary = "OAuth 로그인", description = "인가 코드를 검증하고 회원 생성 또는 조회 후 Access Token을 발급합니다.")
    public ApiResponse<LoginResponse> login(
            @PathVariable String provider,
            @Valid @RequestBody OAuthLoginRequest request) {
        return ApiResponse.of(service.login(OAuthProviderType.from(provider), request));
    }

}
