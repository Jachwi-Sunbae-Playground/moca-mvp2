package com.jachwisunbae.property.domain;

public record PropertyMemo(String content) {

    public static final int MAX_LENGTH = 5_000;

    public PropertyMemo {
        if (!isValid(content)) {
            throw new IllegalArgumentException("매물 메모 길이가 올바르지 않습니다.");
        }
    }

    public static boolean isValid(final String content) {
        return content != null && content.codePointCount(0, content.length()) <= MAX_LENGTH;
    }

    public static PropertyMemo empty() {
        return new PropertyMemo("");
    }
}
