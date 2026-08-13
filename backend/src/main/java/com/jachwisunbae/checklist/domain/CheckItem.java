package com.jachwisunbae.checklist.domain;

import java.util.Objects;

public record CheckItem(
        long id,
        CheckStage stage,
        String question,
        String guide,
        boolean active
) {

    public CheckItem {
        if (id <= 0) {
            throw new IllegalArgumentException("체크 항목 식별자가 올바르지 않습니다.");
        }
        Objects.requireNonNull(stage);
        Objects.requireNonNull(question);
    }
}
