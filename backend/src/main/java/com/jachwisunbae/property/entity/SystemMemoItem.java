package com.jachwisunbae.property.entity;

import lombok.Getter;
import com.jachwisunbae.common.exception.DomainErrorCode;
import com.jachwisunbae.common.validation.DomainPreconditions;

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
        return new SystemMemoItem(id,
                DomainPreconditions.requireTrimmed(label, 1, 30, DomainErrorCode.PROPERTY_MEMO_INVALID,
                        "시스템 메모 항목명은 1자 이상 30자 이하여야 합니다."),
                DomainPreconditions.requirePositive(displayOrder, DomainErrorCode.PROPERTY_MEMO_INVALID,
                        "시스템 메모 표시 순서는 양수여야 합니다."), deletedAt);
    }
}
