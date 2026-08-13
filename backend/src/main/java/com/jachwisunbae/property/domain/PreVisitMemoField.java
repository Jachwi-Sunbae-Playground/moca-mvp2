package com.jachwisunbae.property.domain;

public record PreVisitMemoField(String value) {

    public static final int MAX_LENGTH = 200;

    public PreVisitMemoField {
        if (!isValid(value)) {
            throw new IllegalArgumentException("방문 전 사전 메모 항목이 올바르지 않습니다.");
        }
    }

    public static boolean isValid(final String value) {
        return value != null && value.codePointCount(0, value.length()) <= MAX_LENGTH;
    }

    public static PreVisitMemoField empty() {
        return new PreVisitMemoField("");
    }
}
