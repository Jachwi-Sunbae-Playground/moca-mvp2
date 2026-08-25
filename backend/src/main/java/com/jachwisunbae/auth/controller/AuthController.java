package com.jachwisunbae.auth.controller;

import com.jachwisunbae.auth.controller.dto.LoginResponse;
import com.jachwisunbae.auth.controller.dto.NicknameLoginRequest;
import com.jachwisunbae.auth.service.AuthService;
import com.jachwisunbae.common.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "닉네임 로그인과 Access Token 발급 API")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/nickname")
    @Operation(summary = "닉네임 로그인",
            description = "처음 보는 닉네임은 선택 비밀번호와 함께 생성하고, 기존 닉네임은 같은 자격정보로 로그인합니다.")
    public ApiResponse<LoginResponse> loginNickname(@Valid @RequestBody NicknameLoginRequest request) {
        return ApiResponse.of(service.loginNickname(request));
    }

}
