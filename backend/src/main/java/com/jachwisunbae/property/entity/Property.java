package com.jachwisunbae.property.entity;

import lombok.Getter;
import com.jachwisunbae.common.entity.BaseTimeEntity;

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
        return new Property(null, memberId, name, depositAmount, monthlyRentAmount, discoverySource, now, now);
    }

    public static Property reconstruct(final Long id, final Long memberId, final String name,
                                       final Long depositAmount, final Long monthlyRentAmount,
                                       final String discoverySource, final LocalDateTime createdAt,
                                       final LocalDateTime updatedAt) {
        return new Property(id, memberId, name, depositAmount, monthlyRentAmount, discoverySource, createdAt, updatedAt);
    }

    public void replaceBasicInfo(final String name, final Long depositAmount, final Long monthlyRentAmount,
                                 final String discoverySource, final LocalDateTime now) {
        this.name = name;
        this.depositAmount = depositAmount;
        this.monthlyRentAmount = monthlyRentAmount;
        this.discoverySource = discoverySource;
        updateUpdatedAt(now);
    }
}
