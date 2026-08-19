package com.jachwisunbae.property.entity;

import lombok.Getter;

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
        return new PropertyMemo(null, propertyId, freeMemo);
    }

    public static PropertyMemo reconstruct(final Long id, final Long propertyId, final String freeMemo) {
        return new PropertyMemo(id, propertyId, freeMemo);
    }

    public void replaceFreeMemo(final String freeMemo) {
        this.freeMemo = freeMemo;
    }
}
