package com.jachwisunbae.checklist.entity;

import lombok.Getter;
import com.jachwisunbae.checklist.type.CheckStage;
import com.jachwisunbae.common.exception.DomainErrorCode;
import com.jachwisunbae.common.validation.DomainPreconditions;

@Getter
public class UserChecklist {

    private final Long id;
    private final Long memberId;
    private String name;
    private final CheckStage stage;

    private UserChecklist(final Long id, final Long memberId, final String name, final CheckStage stage) {
        this.id = id;
        this.memberId = memberId;
        this.name = name;
        this.stage = stage;
    }

    public static UserChecklist create(final Long memberId, final String name, final CheckStage stage) {
        return new UserChecklist(null, validateMemberId(memberId), validateName(name), validateStage(stage));
    }

    public static UserChecklist reconstruct(final Long id, final Long memberId, final String name,
                                            final CheckStage stage) {
        return new UserChecklist(id, validateMemberId(memberId), validateName(name), validateStage(stage));
    }

    public void rename(final String name) {
        this.name = validateName(name);
    }

    private static Long validateMemberId(final Long memberId) {
        return DomainPreconditions.requireNonNull(memberId, DomainErrorCode.USER_CHECKLIST_MEMBER_REQUIRED,
                "체크리스트 소유 회원은 필수입니다.");
    }

    private static String validateName(final String name) {
        return DomainPreconditions.requireTrimmed(name, 1, 30, DomainErrorCode.USER_CHECKLIST_NAME_INVALID,
                "체크리스트 이름은 trim 후 1자 이상 30자 이하여야 합니다.");
    }

    private static CheckStage validateStage(final CheckStage stage) {
        return DomainPreconditions.requireNonNull(stage, DomainErrorCode.USER_CHECKLIST_STAGE_REQUIRED,
                "체크리스트 단계는 필수입니다.");
    }
}
