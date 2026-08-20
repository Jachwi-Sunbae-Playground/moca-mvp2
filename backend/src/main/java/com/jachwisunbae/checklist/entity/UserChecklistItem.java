package com.jachwisunbae.checklist.entity;

import lombok.Getter;
import com.jachwisunbae.common.exception.DomainErrorCode;
import com.jachwisunbae.common.validation.DomainPreconditions;
import com.jachwisunbae.checklist.type.CheckStage;
import com.jachwisunbae.checklist.type.CheckItemType;

@Getter
public class UserChecklistItem {

    private final Long id;
    private final Long userChecklistId;
    private final Long systemCheckItemId;
    private final CheckStage stage;
    private final CheckItemType itemType;
    private final String question;
    private final Integer displayOrder;

    private UserChecklistItem(final Long id, final Long userChecklistId, final Long systemCheckItemId,
                               final CheckStage stage, final CheckItemType itemType, final String question,
                               final Integer displayOrder) {
        this.id = id;
        this.userChecklistId = userChecklistId;
        this.systemCheckItemId = systemCheckItemId;
        this.stage = stage;
        this.itemType = itemType;
        this.question = question;
        this.displayOrder = displayOrder;
    }

    public static UserChecklistItem create(final Long userChecklistId, final SystemCheckItem systemItem,
                                           final Integer displayOrder) {
        return new UserChecklistItem(null, validateId(userChecklistId), validateId(systemItem.getId()),
                systemItem.getStage(), systemItem.getItemType(), systemItem.getQuestion(), validateOrder(displayOrder));
    }

    public static UserChecklistItem reconstruct(final Long id, final Long userChecklistId,
                                               final Long systemCheckItemId, final CheckStage stage,
                                               final CheckItemType itemType, final String question,
                                               final Integer displayOrder) {
        return new UserChecklistItem(id, validateId(userChecklistId), validateId(systemCheckItemId), stage,
                itemType, question, validateOrder(displayOrder));
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
