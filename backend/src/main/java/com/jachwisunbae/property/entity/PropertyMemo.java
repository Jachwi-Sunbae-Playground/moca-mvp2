package com.jachwisunbae.property.entity;

import lombok.Getter;
import com.jachwisunbae.common.exception.DomainErrorCode;
import com.jachwisunbae.common.validation.DomainPreconditions;

@Getter
public class PropertyMemo {

    private final Long id;
    private final Long propertyId;
    private String freeMemo;

    private PropertyMemo(final Long id, final Long propertyId, final String freeMemo) {
        this.id = id;
        this.propertyId = propertyId;
        this.freeMemo = freeMemo;
    }

    public static PropertyMemo create(final Long propertyId, final String freeMemo) {
        return new PropertyMemo(null, validatePropertyId(propertyId), validateFreeMemo(freeMemo));
    }

    public static PropertyMemo reconstruct(final Long id, final Long propertyId, final String freeMemo) {
        return new PropertyMemo(id, validatePropertyId(propertyId), validateFreeMemo(freeMemo));
    }

    public void replaceFreeMemo(final String freeMemo) {
        this.freeMemo = validateFreeMemo(freeMemo);
    }

    private static Long validatePropertyId(final Long propertyId) {
        return DomainPreconditions.requireNonNull(propertyId, DomainErrorCode.PROPERTY_MEMO_INVALID,
                "메모의 매물 ID는 필수입니다.");
    }

    private static String validateFreeMemo(final String freeMemo) {
        String value = defaultMemo(freeMemo);
        DomainPreconditions.require(value.length() <= 2000, DomainErrorCode.PROPERTY_MEMO_INVALID,
                "자유 메모는 2,000자 이하여야 합니다.");
        return value;
    }

    private static String defaultMemo(final String memo) {
        if (memo == null) {
            return "";
        }
        return memo;
    }
}
