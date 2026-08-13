package com.jachwisunbae.checklist.domain;

import java.util.Objects;

public record ChecklistItem(
        long id,
        ChecklistItemOrigin origin,
        Long sourceCheckItemId,
        String customQuestion,
        CheckStage stage,
        int order
) {

    public ChecklistItem {
        if (id < 0 || order < 1) {
            throw new IllegalArgumentException("체크리스트 항목이 올바르지 않습니다.");
        }
        Objects.requireNonNull(origin);
        Objects.requireNonNull(stage);
        if (origin == ChecklistItemOrigin.PROVIDED) {
            if (sourceCheckItemId == null || sourceCheckItemId <= 0 || customQuestion != null) {
                throw new IllegalArgumentException("제공 체크리스트 항목이 올바르지 않습니다.");
            }
        } else {
            customQuestion = normalizeCustomQuestion(customQuestion);
            if (sourceCheckItemId != null || customQuestion == null) {
                throw new IllegalArgumentException("사용자 체크리스트 항목이 올바르지 않습니다.");
            }
        }
    }

    public ChecklistItem(final long checkItemId, final CheckStage stage, final int order) {
        this(0, ChecklistItemOrigin.PROVIDED, checkItemId, null, stage, order);
    }

    public static ChecklistItem provided(
            final long id,
            final long sourceCheckItemId,
            final CheckStage stage,
            final int order
    ) {
        return new ChecklistItem(id, ChecklistItemOrigin.PROVIDED, sourceCheckItemId, null, stage, order);
    }

    public static ChecklistItem custom(
            final long id,
            final String customQuestion,
            final CheckStage stage,
            final int order
    ) {
        return new ChecklistItem(id, ChecklistItemOrigin.CUSTOM, null, customQuestion, stage, order);
    }

    public static String normalizeCustomQuestion(final String question) {
        if (question == null) {
            return null;
        }
        final String normalized = question.trim();
        if (normalized.isEmpty() || normalized.codePointCount(0, normalized.length()) > 200) {
            return null;
        }
        return normalized;
    }

}
