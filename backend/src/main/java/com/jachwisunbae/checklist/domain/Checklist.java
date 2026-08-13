package com.jachwisunbae.checklist.domain;

import com.jachwisunbae.common.exception.client.BusinessRuleViolationException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record Checklist(
        long id,
        long memberId,
        ChecklistName name,
        CheckStage stage,
        List<ChecklistItem> items,
        Instant createdAt,
        Instant updatedAt
) {

    public Checklist {
        if (id < 0 || memberId <= 0) {
            throw new IllegalArgumentException("체크리스트 식별자가 올바르지 않습니다.");
        }
        Objects.requireNonNull(name);
        Objects.requireNonNull(stage);
        items = List.copyOf(items);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(updatedAt);
        validateItems(stage, items);
    }

    public Checklist withId(final long checklistId) {
        return new Checklist(checklistId, memberId, name, stage, items, createdAt, updatedAt);
    }

    private static void validateItems(final CheckStage stage, final List<ChecklistItem> items) {
        if (items.isEmpty()) {
            throw new BusinessRuleViolationException(ErrorCode.CHECKLIST_EMPTY);
        }
        final Set<Long> sourceIds = new HashSet<>();
        final Set<Long> localIds = new HashSet<>();
        final Set<Integer> orders = new HashSet<>();
        for (final ChecklistItem item : items) {
            if (item.stage() != stage) {
                throw new BusinessRuleViolationException(ErrorCode.CHECKLIST_ITEM_STAGE_MISMATCH);
            }
            if (item.origin() == ChecklistItemOrigin.PROVIDED && !sourceIds.add(item.sourceCheckItemId())) {
                throw new BusinessRuleViolationException(ErrorCode.CHECKLIST_ITEM_DUPLICATED);
            }
            if (item.id() > 0 && !localIds.add(item.id())) {
                throw new BusinessRuleViolationException(ErrorCode.CHECKLIST_ITEM_DUPLICATED);
            }
            if (!orders.add(item.order())) {
                throw new IllegalArgumentException("체크리스트 항목 순서가 중복되었습니다.");
            }
        }
        for (int order = 1; order <= items.size(); order++) {
            if (!orders.contains(order)) {
                throw new IllegalArgumentException("체크리스트 항목 순서가 연속적이지 않습니다.");
            }
        }
    }
}
