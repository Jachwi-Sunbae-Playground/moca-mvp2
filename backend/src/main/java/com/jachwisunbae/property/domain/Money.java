package com.jachwisunbae.property.domain;

public record Money(long amount) {

    public static final long MAX_AMOUNT = 9_007_199_254_740_991L;

    public Money {
        if (amount < 0 || amount > MAX_AMOUNT) {
            throw new IllegalArgumentException("금액 범위가 올바르지 않습니다.");
        }
    }
}
