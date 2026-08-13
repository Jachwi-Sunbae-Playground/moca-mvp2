package com.jachwisunbae.visit.repository;

import com.jachwisunbae.visit.domain.InlineMemo;
import java.time.Instant;

public record VisitItemMemoStateProjection(
        long visitItemId,
        InlineMemo memo,
        long memoVersion,
        Instant memoSavedAt
) {
}
