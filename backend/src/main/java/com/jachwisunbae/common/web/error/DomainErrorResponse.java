package com.jachwisunbae.common.web.error;

import java.util.List;

public record DomainErrorResponse(
        String code,
        String message,
        List<FieldErrorResponse> errors) {

    public DomainErrorResponse {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public DomainErrorResponse(final String code, final String message) {
        this(code, message, List.of());
    }
}
