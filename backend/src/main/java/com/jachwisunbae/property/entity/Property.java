package com.jachwisunbae.property.entity;

import lombok.Getter;
import com.jachwisunbae.common.entity.BaseTimeEntity;
import com.jachwisunbae.common.exception.DomainErrorCode;
import com.jachwisunbae.common.validation.DomainPreconditions;

import java.time.LocalDateTime;

@Getter
public class Property extends BaseTimeEntity {

    private final Long id;
    private final Long memberId;
    private String name;
    private Long depositAmount;
    private Long monthlyRentAmount;
    private String discoverySource;

    private Property(final Long id, final Long memberId, final String name,
                     final Long depositAmount, final Long monthlyRentAmount,
                     final String discoverySource, final LocalDateTime createdAt,
                     final LocalDateTime updatedAt) {
        super(createdAt, updatedAt);
        this.id = id;
        this.memberId = memberId;
        this.name = name;
        this.depositAmount = depositAmount;
        this.monthlyRentAmount = monthlyRentAmount;
        this.discoverySource = discoverySource;
    }

    public static Property create(final Long memberId, final String name, final Long depositAmount,
                                  final Long monthlyRentAmount, final String discoverySource,
                                  final LocalDateTime now) {
        return new Property(null, validateMemberId(memberId), validateName(name), validateAmount(depositAmount),
                validateAmount(monthlyRentAmount), validateSource(discoverySource), now, now);
    }

    public static Property reconstruct(final Long id, final Long memberId, final String name,
                                       final Long depositAmount, final Long monthlyRentAmount,
                                       final String discoverySource, final LocalDateTime createdAt,
                                       final LocalDateTime updatedAt) {
        return new Property(id, validateMemberId(memberId), validateName(name), validateAmount(depositAmount),
                validateAmount(monthlyRentAmount), validateSource(discoverySource), createdAt, updatedAt);
    }

    public void replaceBasicInfo(final String name, final Long depositAmount, final Long monthlyRentAmount,
                                 final String discoverySource, final LocalDateTime now) {
        this.name = validateName(name);
        this.depositAmount = validateAmount(depositAmount);
        this.monthlyRentAmount = validateAmount(monthlyRentAmount);
        this.discoverySource = validateSource(discoverySource);
        updateUpdatedAt(now);
    }

    private static Long validateMemberId(final Long memberId) {
        return DomainPreconditions.requireNonNull(memberId, DomainErrorCode.PROPERTY_INPUT_INVALID,
                "매물 소유 회원은 필수입니다.");
    }

    private static String validateName(final String name) {
        return DomainPreconditions.requireTrimmed(name, 1, 30, DomainErrorCode.PROPERTY_INPUT_INVALID,
                "매물 이름은 trim 후 1자 이상 30자 이하여야 합니다.");
    }

    private static Long validateAmount(final Long amount) {
        if (amount == null) {
            return null;
        }
        return DomainPreconditions.requireNonNegative(amount, DomainErrorCode.PROPERTY_INPUT_INVALID,
                "보증금과 월세는 0 이상의 정수여야 합니다.");
    }

    private static String validateSource(final String source) {
        if (source == null) {
            return null;
        }
        DomainPreconditions.require(source.length() <= 500, DomainErrorCode.PROPERTY_INPUT_INVALID,
                "발견 경로는 500자 이하여야 합니다.");
        return source;
    }
}
