package com.jachwisunbae.visit.service.dto.result;

import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.checklist.domain.ChecklistItemOrigin;
import com.jachwisunbae.visit.domain.VisitSummary;
import com.jachwisunbae.visit.repository.VisitDetailRow;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record VisitDetailResult(
        long visitId,
        long propertyId,
        String status,
        Instant startedAt,
        Instant completedAt,
        Instant updatedAt,
        List<VisitStageResult> stages,
        VisitSummaryResult summary
) {

    public VisitDetailResult {
        stages = List.copyOf(stages);
    }

    public static VisitDetailResult from(final List<VisitDetailRow> rows) {
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("방문 상세 행이 필요합니다.");
        }
        final VisitDetailRow first = rows.getFirst();
        final Map<Long, StageBuilder> stages = new LinkedHashMap<>();
        for (VisitDetailRow row : rows) {
            stages.computeIfAbsent(row.snapshotId(), ignored -> new StageBuilder(
                    row.stage(),
                    row.sourceChecklistId(),
                    row.checklistName()
            )).add(row);
        }
        final List<VisitStageResult> stageResults = stages.values().stream()
                .map(StageBuilder::build)
                .toList();
        final VisitSummary total = combine(stageResults);
        return new VisitDetailResult(
                first.visitId(),
                first.propertyId(),
                first.visitStatus().name(),
                first.startedAt(),
                first.completedAt(),
                first.updatedAt(),
                stageResults,
                VisitSummaryResult.from(total)
        );
    }

    private static VisitSummary combine(final List<VisitStageResult> stages) {
        final int total = stages.stream().mapToInt(stage -> stage.summary().totalCount()).sum();
        final int good = stages.stream().mapToInt(stage -> stage.summary().goodCount()).sum();
        final int caution = stages.stream().mapToInt(stage -> stage.summary().cautionCount()).sum();
        final int unconfirmed = stages.stream().mapToInt(stage -> stage.summary().unconfirmedCount()).sum();
        return VisitSummary.from(total, good, caution, unconfirmed);
    }

    public record VisitStageResult(
            CheckStage stage,
            Long sourceChecklistId,
            String checklistName,
            List<VisitItemResult> items,
            VisitSummaryResult summary
    ) {

        public VisitStageResult {
            items = List.copyOf(items);
        }
    }

    public record VisitItemResult(
            long visitItemId,
            ChecklistItemOrigin origin,
            Long sourceChecklistItemId,
            Long sourceCheckItemId,
            String question,
            String guide,
            int order,
            String status,
            long statusVersion,
            Instant statusSavedAt,
            String inlineMemo,
            long memoVersion,
            Instant memoSavedAt
    ) {
    }

    private static final class StageBuilder {

        private final CheckStage stage;
        private final Long sourceChecklistId;
        private final String checklistName;
        private final List<VisitItemResult> items = new ArrayList<>();

        private StageBuilder(
                final CheckStage stage,
                final Long sourceChecklistId,
                final String checklistName
        ) {
            this.stage = stage;
            this.sourceChecklistId = sourceChecklistId;
            this.checklistName = checklistName;
        }

        private void add(final VisitDetailRow row) {
            items.add(new VisitItemResult(
                    row.visitItemId(),
                    row.origin(),
                    row.sourceChecklistItemId(),
                    row.sourceCheckItemId(),
                    row.question(),
                    row.guide(),
                    row.order(),
                    row.itemStatus().name(),
                    row.statusVersion(),
                    row.statusSavedAt(),
                    row.inlineMemo(),
                    row.memoVersion(),
                    row.memoSavedAt()
            ));
        }

        private VisitStageResult build() {
            final int total = items.size();
            final int good = (int) items.stream().filter(item -> "GOOD".equals(item.status())).count();
            final int caution = (int) items.stream().filter(item -> "CAUTION".equals(item.status())).count();
            final int unconfirmed = (int) items.stream().filter(item -> "UNCONFIRMED".equals(item.status())).count();
            return new VisitStageResult(
                    stage,
                    sourceChecklistId,
                    checklistName,
                    items,
                    VisitSummaryResult.from(VisitSummary.from(total, good, caution, unconfirmed))
            );
        }
    }
}
