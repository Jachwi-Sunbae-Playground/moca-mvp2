package com.jachwisunbae.visit.domain;

import com.jachwisunbae.checklist.domain.ChecklistItemOrigin;
import com.jachwisunbae.common.exception.client.BusinessRuleViolationException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import java.time.Instant;
import java.util.Objects;

public record VisitCheckItem(
        long id,
        ChecklistItemOrigin origin,
        Long sourceChecklistItemId,
        Long sourceCheckItemId,
        String question,
        String guide,
        int order,
        CheckStatus status,
        long statusVersion,
        Instant statusSavedAt,
        InlineMemo inlineMemo,
        long memoVersion,
        Instant memoSavedAt
) {

    public VisitCheckItem {
        if (id < 0
                || sourceChecklistItemId != null && sourceChecklistItemId <= 0
                || order <= 0
                || statusVersion < 0
                || memoVersion < 0
                || memoVersion > 0 && memoSavedAt == null) {
            throw new IllegalArgumentException("방문 체크 항목 값이 올바르지 않습니다.");
        }
        Objects.requireNonNull(origin);
        Objects.requireNonNull(question);
        Objects.requireNonNull(status);
        Objects.requireNonNull(statusSavedAt);
        Objects.requireNonNull(inlineMemo);
        if (origin == ChecklistItemOrigin.PROVIDED && (sourceCheckItemId == null || sourceCheckItemId <= 0)) {
            throw new IllegalArgumentException("제공 방문 체크 항목 출처가 올바르지 않습니다.");
        }
        if (origin == ChecklistItemOrigin.CUSTOM && (sourceCheckItemId != null || guide != null)) {
            throw new IllegalArgumentException("사용자 방문 체크 항목 출처가 올바르지 않습니다.");
        }
    }

    public VisitCheckItem changeStatus(
            final CheckStatus changedStatus,
            final long expectedStatusVersion,
            final Instant savedAt
    ) {
        if (statusVersion != expectedStatusVersion) {
            throw new BusinessRuleViolationException(ErrorCode.VISIT_ITEM_STATUS_VERSION_CONFLICT);
        }
        return new VisitCheckItem(
                id,
                origin,
                sourceChecklistItemId,
                sourceCheckItemId,
                question,
                guide,
                order,
                Objects.requireNonNull(changedStatus),
                statusVersion + 1,
                Objects.requireNonNull(savedAt),
                inlineMemo,
                memoVersion,
                memoSavedAt
        );
    }

    public VisitCheckItem changeMemo(
            final InlineMemo changedMemo,
            final long expectedMemoVersion,
            final Instant savedAt
    ) {
        if (memoVersion != expectedMemoVersion) {
            throw new BusinessRuleViolationException(ErrorCode.VISIT_ITEM_MEMO_VERSION_CONFLICT);
        }
        return new VisitCheckItem(
                id,
                origin,
                sourceChecklistItemId,
                sourceCheckItemId,
                question,
                guide,
                order,
                status,
                statusVersion,
                statusSavedAt,
                Objects.requireNonNull(changedMemo),
                memoVersion + 1,
                Objects.requireNonNull(savedAt)
        );
    }
}
