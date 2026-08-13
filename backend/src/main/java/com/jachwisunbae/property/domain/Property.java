package com.jachwisunbae.property.domain;

import java.time.Instant;
import java.util.Objects;

public record Property(
        long id,
        long memberId,
        PropertyName name,
        Money depositAmount,
        Money monthlyRentAmount,
        DiscoverySource discoverySource,
        PropertyMemo memo,
        Instant memoUpdatedAt,
        Instant lastActivityAt,
        Instant createdAt,
        Instant updatedAt
) {

    public Property {
        if (id < 0 || memberId <= 0) {
            throw new IllegalArgumentException("매물 식별자가 올바르지 않습니다.");
        }
        Objects.requireNonNull(name);
        Objects.requireNonNull(depositAmount);
        Objects.requireNonNull(monthlyRentAmount);
        Objects.requireNonNull(discoverySource);
        Objects.requireNonNull(memo);
        Objects.requireNonNull(lastActivityAt);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(updatedAt);
    }

    public static Property create(
            final long memberId,
            final PropertyName name,
            final Money depositAmount,
            final Money monthlyRentAmount,
            final DiscoverySource discoverySource,
            final Instant now
    ) {
        return new Property(
                0,
                memberId,
                name,
                depositAmount,
                monthlyRentAmount,
                discoverySource,
                PropertyMemo.empty(),
                null,
                now,
                now,
                now
        );
    }

    public Property withId(final long propertyId) {
        return new Property(
                propertyId,
                memberId,
                name,
                depositAmount,
                monthlyRentAmount,
                discoverySource,
                memo,
                memoUpdatedAt,
                lastActivityAt,
                createdAt,
                updatedAt
        );
    }

    public Property updateBasicInfo(
            final PropertyName changedName,
            final Money changedDepositAmount,
            final Money changedMonthlyRentAmount,
            final DiscoverySource changedDiscoverySource,
            final Instant now
    ) {
        return new Property(
                id,
                memberId,
                changedName == null ? name : changedName,
                changedDepositAmount == null ? depositAmount : changedDepositAmount,
                changedMonthlyRentAmount == null ? monthlyRentAmount : changedMonthlyRentAmount,
                changedDiscoverySource == null ? discoverySource : changedDiscoverySource,
                memo,
                memoUpdatedAt,
                now,
                createdAt,
                now
        );
    }

    public Property updateMemo(final PropertyMemo changedMemo, final Instant now) {
        return new Property(
                id,
                memberId,
                name,
                depositAmount,
                monthlyRentAmount,
                discoverySource,
                changedMemo,
                now,
                now,
                createdAt,
                now
        );
    }
}
