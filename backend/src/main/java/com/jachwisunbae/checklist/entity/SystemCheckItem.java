package com.jachwisunbae.checklist.entity;

import lombok.Getter;
import com.jachwisunbae.checklist.type.CheckItemType;
import com.jachwisunbae.checklist.type.CheckStage;

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
        return new SystemCheckItem(id, stage, itemType, question, deletedAt);
    }
}
