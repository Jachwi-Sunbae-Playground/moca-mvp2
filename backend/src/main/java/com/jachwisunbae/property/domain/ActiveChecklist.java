package com.jachwisunbae.property.domain;

import com.jachwisunbae.checklist.domain.CheckStage;
import java.time.Instant;
import java.util.Objects;

public record ActiveChecklist(
        long propertyId,
        long memberId,
        CheckStage stage,
        long checklistId,
        Instant createdAt,
        Instant updatedAt
) {

    public ActiveChecklist {
        if (propertyId <= 0 || memberId <= 0 || checklistId <= 0) {
            throw new IllegalArgumentException("활성 체크리스트 식별자는 양수여야 합니다.");
        }
        Objects.requireNonNull(stage);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(updatedAt);
    }

    public static ActiveChecklist create(
            final long propertyId,
            final long memberId,
            final CheckStage stage,
            final long checklistId,
            final Instant now
    ) {
        return new ActiveChecklist(propertyId, memberId, stage, checklistId, now, now);
    }

    public boolean uses(final long otherChecklistId) {
        return checklistId == otherChecklistId;
    }
}
