package com.jachwisunbae.property.domain;

public record PropertyName(String value) {

    private static final int MAX_LENGTH = 50;

    public PropertyName {
        if (value == null) {
            throw new IllegalArgumentException("매물 이름은 필수입니다.");
        }
        value = value.trim();
        final int length = value.codePointCount(0, value.length());
        if (length < 1 || length > MAX_LENGTH) {
            throw new IllegalArgumentException("매물 이름 길이가 올바르지 않습니다.");
        }
    }
}
