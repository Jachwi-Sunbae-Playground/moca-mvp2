package com.jachwisunbae.property.repository.query;

public record PropertyProgressSummary(Long propertyId, int totalCount, int completedCount, int goodCount,
                                      int cautionCount, int unconfirmedCount) {
    public int progressRate() {
        if (totalCount == 0) {
            return 0;
        }
        return completedCount * 100 / totalCount;
    }
}
