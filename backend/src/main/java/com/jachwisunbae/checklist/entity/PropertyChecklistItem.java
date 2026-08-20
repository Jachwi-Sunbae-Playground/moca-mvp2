package com.jachwisunbae.checklist.entity;

import lombok.Getter;
import com.jachwisunbae.checklist.type.CheckStatus;
import com.jachwisunbae.common.exception.DomainErrorCode;
import com.jachwisunbae.common.validation.DomainPreconditions;

@Getter
public class PropertyChecklistItem {

    private final Long id;
    private final Long propertyChecklistId;
    private final Long systemCheckItemId;
    private Integer displayOrder;
    private CheckStatus status;
    private String memo;
    private final String question;

    private PropertyChecklistItem(final Long id, final Long propertyChecklistId, final Long systemCheckItemId,
                                  final Integer displayOrder, final CheckStatus status, final String memo,
                                  final String question) {
        this.id = id;
        this.propertyChecklistId = propertyChecklistId;
        this.systemCheckItemId = systemCheckItemId;
        this.displayOrder = displayOrder;
        this.status = status;
        this.memo = memo;
        this.question = question;
    }

    public static PropertyChecklistItem create(final Long propertyChecklistId, final Long systemCheckItemId,
                                              final Integer displayOrder, final String question) {
        return new PropertyChecklistItem(null, validateId(propertyChecklistId), validateId(systemCheckItemId),
                validateOrder(displayOrder), CheckStatus.UNCONFIRMED, "", validateQuestion(question));
    }

    public static PropertyChecklistItem reconstruct(final Long id, final Long propertyChecklistId,
                                                    final Long systemCheckItemId, final Integer displayOrder,
                                                    final CheckStatus status, final String memo,
                                                    final String question) {
        return new PropertyChecklistItem(id, validateId(propertyChecklistId), validateId(systemCheckItemId),
                validateOrder(displayOrder), validateStatus(status), validateMemo(memo), validateQuestion(question));
    }

    public void changeStatus(final CheckStatus status) {
        this.status = validateStatus(status);
    }

    public void changeMemo(final String memo) {
        this.memo = validateMemo(memo);
    }

    public void reorder(final Integer displayOrder) {
        this.displayOrder = validateOrder(displayOrder);
    }

    private static Long validateId(final Long id) {
        return DomainPreconditions.requireNonNull(id, DomainErrorCode.PROPERTY_CHECKLIST_ITEM_NOT_FOUND,
                "매물 체크 항목 ID는 필수입니다.");
    }

    private static Integer validateOrder(final Integer order) {
        return DomainPreconditions.requirePositive(order, DomainErrorCode.PROPERTY_CHECK_RESULT_INVALID,
                "표시 순서는 양수여야 합니다.");
    }

    private static CheckStatus validateStatus(final CheckStatus status) {
        return DomainPreconditions.requireNonNull(status, DomainErrorCode.PROPERTY_CHECK_RESULT_INVALID,
                "체크 상태는 필수입니다.");
    }

    private static String validateMemo(final String memo) {
        String value = defaultMemo(memo);
        DomainPreconditions.require(value.length() <= 500, DomainErrorCode.PROPERTY_CHECK_RESULT_INVALID,
                "항목 메모는 500자 이하여야 합니다.");
        return value;
    }

    private static String defaultMemo(final String memo) {
        if (memo == null) {
            return "";
        }
        return memo;
    }

    private static String validateQuestion(final String question) {
        return DomainPreconditions.requireTrimmed(question, 1, 200, DomainErrorCode.PROPERTY_CHECK_RESULT_INVALID,
                "스냅샷 질문은 trim 후 1자 이상 200자 이하여야 합니다.");
    }
}
