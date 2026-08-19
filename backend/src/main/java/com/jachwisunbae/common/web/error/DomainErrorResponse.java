package com.jachwisunbae.common.web.error;

public record DomainErrorResponse(
        String code,
        String message) {
}
