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

    public static UserChecklistItem createCustom(final Long userChecklistId, final CheckStage stage,
                                                 final String question, final Integer displayOrder) {
        return new UserChecklistItem(null, validateId(userChecklistId), null,
                DomainPreconditions.requireNonNull(stage, DomainErrorCode.USER_CHECKLIST_STAGE_REQUIRED,
                        "체크리스트 단계는 필수입니다."),
                CheckItemType.OPTIONAL, validateQuestion(question), validateOrder(displayOrder));
    }

    public static UserChecklistItem reconstruct(final Long id, final Long userChecklistId,
                                               final Long systemCheckItemId, final CheckStage stage,
                                               final CheckItemType itemType, final String question,
                                               final Integer displayOrder) {
        return new UserChecklistItem(id, validateId(userChecklistId), systemCheckItemId,
                DomainPreconditions.requireNonNull(stage, DomainErrorCode.USER_CHECKLIST_STAGE_REQUIRED,
                        "체크리스트 단계는 필수입니다."),
                DomainPreconditions.requireNonNull(itemType, DomainErrorCode.CHECKLIST_ITEMS_INVALID,
                        "체크리스트 항목 유형은 필수입니다."),
                validateQuestion(question), validateOrder(displayOrder));
    }

    public boolean isCustom() {
        return systemCheckItemId == null;
    }

    private static Long validateId(final Long id) {
        return DomainPreconditions.requireNonNull(id, DomainErrorCode.CHECKLIST_ITEMS_INVALID,
                "체크리스트 항목 ID는 필수입니다.");
    }

    private static Integer validateOrder(final Integer order) {
        return DomainPreconditions.requirePositive(order, DomainErrorCode.CHECKLIST_ITEMS_INVALID,
                "표시 순서는 양수여야 합니다.");
    }

    private static String validateQuestion(final String question) {
        return DomainPreconditions.requireTrimmed(question, 1, 200, DomainErrorCode.CHECKLIST_ITEMS_INVALID,
                "직접 추가 질문은 trim 후 1자 이상 200자 이하여야 합니다.");
    }
}
