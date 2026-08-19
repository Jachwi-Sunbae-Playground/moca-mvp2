package com.jachwisunbae.checklist.entity;

import lombok.Getter;
import com.jachwisunbae.checklist.type.CheckStage;

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
        return new UserChecklist(null, memberId, name, stage);
    }

    public static UserChecklist reconstruct(final Long id, final Long memberId, final String name,
                                            final CheckStage stage) {
        return new UserChecklist(id, memberId, name, stage);
    }

    public void rename(final String name) {
        this.name = name;
    }
}
