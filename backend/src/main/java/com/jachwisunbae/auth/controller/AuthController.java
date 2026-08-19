package com.jachwisunbae.auth.controller;

import com.jachwisunbae.auth.controller.dto.LoginResponse;
import com.jachwisunbae.auth.controller.dto.OAuthLoginRequest;
import com.jachwisunbae.auth.provider.OAuthProviderType;
import com.jachwisunbae.auth.service.AuthService;
import com.jachwisunbae.common.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/{provider}")
    public ApiResponse<LoginResponse> login(
            @PathVariable String provider,
            @Valid @RequestBody OAuthLoginRequest request) {
        return ApiResponse.of(service.login(OAuthProviderType.from(provider), request));
    }

}
