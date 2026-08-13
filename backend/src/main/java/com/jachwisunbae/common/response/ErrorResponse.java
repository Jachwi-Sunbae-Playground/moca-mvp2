package com.jachwisunbae.common.response;

import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import java.util.List;

public record ErrorResponse(String code, String message, List<FieldErrorResponse> errors) {

    public ErrorResponse {
        errors = List.copyOf(errors);
    }

    public static ErrorResponse from(final ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), List.of());
    }

    public static ErrorResponse withErrors(
            final ErrorCode errorCode,
            final List<FieldErrorResponse> errors
    ) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), errors);
    }
}
