package com.jachwisunbae.checklist.entity;

import lombok.Getter;

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
        return new UserChecklistItem(null, userChecklistId, systemCheckItemId, displayOrder);
    }

    public static UserChecklistItem reconstruct(final Long id, final Long userChecklistId,
                                               final Long systemCheckItemId, final Integer displayOrder) {
        return new UserChecklistItem(id, userChecklistId, systemCheckItemId, displayOrder);
    }

    public void reorder(final Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
