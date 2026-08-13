package com.jachwisunbae.common.response;

public record FieldErrorResponse(String field, Object rejectedValue, String reason) {
}
