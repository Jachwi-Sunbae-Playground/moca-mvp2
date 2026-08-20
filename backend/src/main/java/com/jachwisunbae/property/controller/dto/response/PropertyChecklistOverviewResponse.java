package com.jachwisunbae.property.controller.dto.response;

import com.jachwisunbae.checklist.type.CheckStage;
import com.jachwisunbae.property.repository.query.PropertyChecklistProgressQuery;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

public record PropertyChecklistOverviewResponse(Long propertyId, PropertyProgress overallProgress,
                                                List<PropertyChecklistStageResponse> stages) {
    public static PropertyChecklistOverviewResponse from(final Long propertyId,
                                                         final List<PropertyChecklistProgressQuery> queries) {
        Map<CheckStage, PropertyChecklistProgressQuery> byStage = queries.stream()
                .collect(Collectors.toMap(PropertyChecklistProgressQuery::stage, Function.identity()));
        List<PropertyChecklistStageResponse> stages = Arrays.stream(CheckStage.values())
                .map(stage -> {
                    PropertyChecklistProgressQuery query = byStage.get(stage);
                    if (query == null) {
                        return new PropertyChecklistStageResponse(stage, false, null, null, null,
                                new PropertyProgress(0, 0, 0, 0, 0, 0));
                    }
                    return new PropertyChecklistStageResponse(stage, true, query.propertyChecklistId(),
                            query.checklistName(), query.sourceChecklistId(), PropertyProgress.from(query.progress()));
                }).toList();
        PropertyProgress overall = sum(stages.stream().map(PropertyChecklistStageResponse::progress).toList());
        return new PropertyChecklistOverviewResponse(propertyId, overall, stages);
    }

    private static PropertyProgress sum(final List<PropertyProgress> progresses) {
        int total = progresses.stream().mapToInt(PropertyProgress::totalCount).sum();
        int completed = progresses.stream().mapToInt(PropertyProgress::completedCount).sum();
        int good = progresses.stream().mapToInt(PropertyProgress::goodCount).sum();
        int caution = progresses.stream().mapToInt(PropertyProgress::cautionCount).sum();
        int unconfirmed = progresses.stream().mapToInt(PropertyProgress::unconfirmedCount).sum();
        return new PropertyProgress(total, completed, good, caution, unconfirmed,
                total == 0 ? 0 : completed * 100 / total);
    }
}
