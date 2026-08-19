package com.jachwisunbae.checklist.entity;

import lombok.Getter;
import com.jachwisunbae.checklist.type.CheckItemType;
import com.jachwisunbae.checklist.type.CheckStage;
import com.jachwisunbae.common.exception.DomainErrorCode;
import com.jachwisunbae.common.validation.DomainPreconditions;

import java.time.LocalDateTime;

@Getter
public class SystemCheckItem {

    private final Long id;
    private final CheckStage stage;
    private final CheckItemType itemType;
    private final String question;
    private final LocalDateTime deletedAt;

    private SystemCheckItem(final Long id, final CheckStage stage, final CheckItemType itemType,
                            final String question, final LocalDateTime deletedAt) {
        this.id = id;
        this.stage = stage;
        this.itemType = itemType;
        this.question = question;
        this.deletedAt = deletedAt;
    }

    public static SystemCheckItem reconstruct(final Long id, final CheckStage stage,
                                              final CheckItemType itemType, final String question,
                                              final LocalDateTime deletedAt) {
        return new SystemCheckItem(id,
                DomainPreconditions.requireNonNull(stage, DomainErrorCode.SYSTEM_CHECK_ITEM_STAGE_REQUIRED,
                        "체크 단계는 필수입니다."),
                DomainPreconditions.requireNonNull(itemType, DomainErrorCode.SYSTEM_CHECK_ITEM_TYPE_REQUIRED,
                        "시스템 항목 유형은 필수입니다."),
                DomainPreconditions.requireTrimmed(question, 1, 200, DomainErrorCode.SYSTEM_CHECK_ITEM_QUESTION_INVALID,
                        "질문은 trim 후 1자 이상 200자 이하여야 합니다."), deletedAt);
    }
}
