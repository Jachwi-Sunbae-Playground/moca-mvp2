package com.jachwisunbae.checklist.entity;

import lombok.Getter;
import com.jachwisunbae.common.exception.DomainErrorCode;
import com.jachwisunbae.common.validation.DomainPreconditions;

@Getter
public class UserChecklistItem {

    private final Long id;
    private final Long userChecklistId;
    private final Long systemCheckItemId;
    private Integer displayOrder;

    private UserChecklistItem(final Long id, final Long userChecklistId, final Long systemCheckItemId,
                               final Integer displayOrder) {
        this.id = id;
        this.userChecklistId = userChecklistId;
        this.systemCheckItemId = systemCheckItemId;
        this.displayOrder = displayOrder;
    }

    public static UserChecklistItem create(final Long userChecklistId, final Long systemCheckItemId,
                                           final Integer displayOrder) {
        return new UserChecklistItem(null, validateId(userChecklistId), validateId(systemCheckItemId),
                validateOrder(displayOrder));
    }

    public static UserChecklistItem reconstruct(final Long id, final Long userChecklistId,
                                               final Long systemCheckItemId, final Integer displayOrder) {
        return new UserChecklistItem(id, validateId(userChecklistId), validateId(systemCheckItemId),
                validateOrder(displayOrder));
    }

    public void reorder(final Integer displayOrder) {
        this.displayOrder = validateOrder(displayOrder);
    }

    private static Long validateId(final Long id) {
        return DomainPreconditions.requireNonNull(id, DomainErrorCode.CHECKLIST_ITEMS_INVALID,
                "체크리스트 항목 ID는 필수입니다.");
    }

    private static Integer validateOrder(final Integer order) {
        return DomainPreconditions.requirePositive(order, DomainErrorCode.CHECKLIST_ITEMS_INVALID,
                "표시 순서는 양수여야 합니다.");
    }
}
