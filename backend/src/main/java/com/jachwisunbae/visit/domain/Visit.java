package com.jachwisunbae.visit.domain;

import java.time.Instant;
import java.util.Objects;

public record Visit(
        long id,
        long propertyId,
        long memberId,
        VisitStatus status,
        Instant startedAt,
        Instant completedAt,
        Instant updatedAt
) {

    public Visit {
        if (propertyId <= 0 || memberId <= 0) {
            throw new IllegalArgumentException("방문의 매물과 회원 식별자는 양수여야 합니다.");
        }
        Objects.requireNonNull(status);
        Objects.requireNonNull(startedAt);
        Objects.requireNonNull(updatedAt);
        if ((status == VisitStatus.IN_PROGRESS && completedAt != null)
                || (status == VisitStatus.COMPLETED && completedAt == null)) {
            throw new IllegalArgumentException("방문 상태와 완료 시각이 일치하지 않습니다.");
        }
    }

    public static Visit start(final long propertyId, final long memberId, final Instant now) {
        return new Visit(0, propertyId, memberId, VisitStatus.IN_PROGRESS, now, null, now);
    }

    public Visit withId(final long newId) {
        return new Visit(newId, propertyId, memberId, status, startedAt, completedAt, updatedAt);
    }

    public Visit complete(final Instant now) {
        if (status == VisitStatus.COMPLETED) {
            return this;
        }
        return new Visit(id, propertyId, memberId, VisitStatus.COMPLETED, startedAt, now, now);
    }
}
