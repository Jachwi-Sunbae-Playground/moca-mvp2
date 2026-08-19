package com.jachwisunbae.checklist.entity;

import lombok.Getter;
import com.jachwisunbae.checklist.type.CheckStatus;

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
        return new PropertyChecklistItem(null, propertyChecklistId, systemCheckItemId, displayOrder,
                CheckStatus.UNCONFIRMED, "", question);
    }

    public static PropertyChecklistItem reconstruct(final Long id, final Long propertyChecklistId,
                                                    final Long systemCheckItemId, final Integer displayOrder,
                                                    final CheckStatus status, final String memo,
                                                    final String question) {
        return new PropertyChecklistItem(id, propertyChecklistId, systemCheckItemId, displayOrder,
                status, memo, question);
    }

    public void changeStatus(final CheckStatus status) {
        this.status = status;
    }

    public void changeMemo(final String memo) {
        this.memo = memo;
    }

    public void reorder(final Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
