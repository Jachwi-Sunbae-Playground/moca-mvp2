package com.jachwisunbae.property.controller.dto.response;

import com.jachwisunbae.property.repository.query.PropertyProgressSummary;

public record PropertyProgress(int totalCount, int completedCount, int goodCount, int cautionCount,
                               int unconfirmedCount, int progressRate) {
    public static PropertyProgress from(final PropertyProgressSummary summary) {
        return new PropertyProgress(summary.totalCount(), summary.completedCount(), summary.goodCount(),
                summary.cautionCount(), summary.unconfirmedCount(), summary.progressRate());
    }

}
