package com.jachwisunbae.property.entity;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class SystemMemoItem {

    private final Long id;
    private final String label;
    private final Integer displayOrder;
    private final LocalDateTime deletedAt;

    private SystemMemoItem(final Long id, final String label, final Integer displayOrder,
                            final LocalDateTime deletedAt) {
        this.id = id;
        this.label = label;
        this.displayOrder = displayOrder;
        this.deletedAt = deletedAt;
    }

    public static SystemMemoItem reconstruct(final Long id, final String label, final Integer displayOrder,
                                             final LocalDateTime deletedAt) {
        return new SystemMemoItem(id, label, displayOrder, deletedAt);
    }
}
