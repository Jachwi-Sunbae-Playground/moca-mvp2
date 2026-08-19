package com.jachwisunbae.checklist.entity;

import lombok.Getter;
import com.jachwisunbae.checklist.type.CheckStage;

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
        return new PropertyChecklist(null, propertyId, userChecklistId, checklistName, stage);
    }

    public static PropertyChecklist reconstruct(final Long id, final Long propertyId, final Long userChecklistId,
                                                final String checklistName, final CheckStage stage) {
        return new PropertyChecklist(id, propertyId, userChecklistId, checklistName, stage);
    }
}
