package com.jachwisunbae.checklist.entity;

import lombok.Getter;
import com.jachwisunbae.checklist.type.CheckStage;
import com.jachwisunbae.common.exception.DomainErrorCode;
import com.jachwisunbae.common.validation.DomainPreconditions;

@Getter
public class PropertyChecklist {

    private final Long id;
    private final Long propertyId;
    private final Long userChecklistId;
    private final String checklistName;
    private final CheckStage stage;

    private PropertyChecklist(final Long id, final Long propertyId, final Long userChecklistId,
                              final String checklistName, final CheckStage stage) {
        this.id = id;
        this.propertyId = propertyId;
        this.userChecklistId = userChecklistId;
        this.checklistName = checklistName;
        this.stage = stage;
    }

    public static PropertyChecklist create(final Long propertyId, final Long userChecklistId,
                                           final String checklistName, final CheckStage stage) {
        return new PropertyChecklist(null, validateId(propertyId), validateNullableId(userChecklistId),
                validateName(checklistName), validateStage(stage));
    }

    public static PropertyChecklist reconstruct(final Long id, final Long propertyId, final Long userChecklistId,
                                                final String checklistName, final CheckStage stage) {
        return new PropertyChecklist(id, validateId(propertyId), validateNullableId(userChecklistId),
                validateName(checklistName), validateStage(stage));
    }

    private static Long validateId(final Long id) {
        return DomainPreconditions.requireNonNull(id, DomainErrorCode.PROPERTY_CHECKLIST_NOT_FOUND,
                "매물 체크리스트 ID는 필수입니다.");
    }

    private static Long validateNullableId(final Long id) {
        return id;
    }

    private static String validateName(final String name) {
        return DomainPreconditions.requireTrimmed(name, 1, 30, DomainErrorCode.CHECKLIST_ITEMS_INVALID,
                "적용 체크리스트 이름은 trim 후 1자 이상 30자 이하여야 합니다.");
    }

    private static CheckStage validateStage(final CheckStage stage) {
        return DomainPreconditions.requireNonNull(stage, DomainErrorCode.PROPERTY_CHECKLIST_STAGE_MISMATCH,
                "적용 단계는 필수입니다.");
    }
}
