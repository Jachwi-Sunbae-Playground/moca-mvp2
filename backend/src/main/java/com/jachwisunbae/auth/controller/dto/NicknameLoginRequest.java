package com.jachwisunbae.auth.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NicknameLoginRequest(
        @NotBlank @Size(max = 100) String nickname,
        @Size(max = 100) String password) {
}
