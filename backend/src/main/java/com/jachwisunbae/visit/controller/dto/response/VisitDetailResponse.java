package com.jachwisunbae.visit.controller.dto.response;

import com.jachwisunbae.checklist.domain.ChecklistItemOrigin;
import com.jachwisunbae.visit.service.dto.result.VisitDetailResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

public record VisitDetailResponse(
        long visitId,
        long propertyId,
        String status,
        Instant startedAt,
        @Schema(nullable = true) Instant completedAt,
        Instant updatedAt,
        List<StageResponse> stages,
        VisitSummaryResponse summary
) {

    public static VisitDetailResponse from(final VisitDetailResult result) {
        return new VisitDetailResponse(
                result.visitId(),
                result.propertyId(),
                result.status(),
                result.startedAt(),
                result.completedAt(),
                result.updatedAt(),
                result.stages().stream().map(StageResponse::from).toList(),
                VisitSummaryResponse.from(result.summary())
        );
    }

    public record StageResponse(
            String stage,
            @Schema(nullable = true) Long sourceChecklistId,
            String checklistName,
            List<ItemResponse> items,
            VisitSummaryResponse summary
    ) {

        static StageResponse from(final VisitDetailResult.VisitStageResult result) {
            return new StageResponse(
                    result.stage().name(),
                    result.sourceChecklistId(),
                    result.checklistName(),
                    result.items().stream().map(ItemResponse::from).toList(),
                    VisitSummaryResponse.from(result.summary())
            );
        }
    }

    @Schema(name = "VisitDetailItemResponse")
    public record ItemResponse(
            long visitItemId,
            ChecklistItemOrigin origin,
            @Schema(nullable = true) Long sourceChecklistItemId,
            @Schema(nullable = true) Long sourceCheckItemId,
            String question,
            @Schema(nullable = true) String guide,
            int order,
            String status,
            long statusVersion,
            Instant statusSavedAt,
            String inlineMemo,
            long memoVersion,
            @Schema(nullable = true) Instant memoSavedAt,
            @Schema(deprecated = true) long version,
            @Schema(deprecated = true) Instant savedAt
    ) {

        static ItemResponse from(final VisitDetailResult.VisitItemResult result) {
            return new ItemResponse(
                    result.visitItemId(),
                    result.origin(),
                    result.sourceChecklistItemId(),
                    result.sourceCheckItemId(),
                    result.question(),
                    result.guide(),
                    result.order(),
                    result.status(),
                    result.statusVersion(),
                    result.statusSavedAt(),
                    result.inlineMemo(),
                    result.memoVersion(),
                    result.memoSavedAt(),
                    result.statusVersion(),
                    result.statusSavedAt()
            );
        }
    }
}
