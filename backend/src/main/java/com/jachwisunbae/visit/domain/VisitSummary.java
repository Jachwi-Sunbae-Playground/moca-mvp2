package com.jachwisunbae.visit.domain;

public record VisitSummary(
        int totalCount,
        int checkedCount,
        int goodCount,
        int cautionCount,
        int unconfirmedCount
) {

    public VisitSummary {
        if (totalCount < 0 || goodCount < 0 || cautionCount < 0 || unconfirmedCount < 0) {
            throw new IllegalArgumentException("방문 집계는 음수일 수 없습니다.");
        }
        if (totalCount != goodCount + cautionCount + unconfirmedCount
                || checkedCount != totalCount - unconfirmedCount) {
            throw new IllegalArgumentException("방문 집계 합계가 일치하지 않습니다.");
        }
    }

    public static VisitSummary from(
            final int totalCount,
            final int goodCount,
            final int cautionCount,
            final int unconfirmedCount
    ) {
        return new VisitSummary(
                totalCount,
                totalCount - unconfirmedCount,
                goodCount,
                cautionCount,
                unconfirmedCount
        );
    }
}
