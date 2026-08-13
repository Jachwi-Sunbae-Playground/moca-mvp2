package com.jachwisunbae.visit.domain;

import com.jachwisunbae.checklist.domain.CheckStage;
import java.time.Instant;
import java.util.List;

public record VisitStageSnapshot(
        long id,
        long visitId,
        CheckStage stage,
        Long sourceChecklistId,
        String checklistName,
        List<VisitCheckItem> items,
        Instant createdAt
) {

    public VisitStageSnapshot {
        items = List.copyOf(items);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("방문 단계 스냅샷에는 항목이 필요합니다.");
        }
    }
}
