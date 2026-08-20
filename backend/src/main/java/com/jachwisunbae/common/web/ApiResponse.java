package com.jachwisunbae.common.web;

public record ApiResponse<T>(String code, String message, T data) {

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>("SUCCESS", "요청이 성공했습니다.", data);
    }

    public static <T> ApiResponse<T> of(final String message, final T data) {
        return new ApiResponse<>("SUCCESS", message, data);
    }
}
