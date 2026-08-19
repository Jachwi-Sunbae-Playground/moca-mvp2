package com.jachwisunbae.property.entity;

import lombok.Getter;

@Getter
public class PropertyMemoItem {

    private final Long id;
    private final Long propertyMemoId;
    private final Long systemMenoId;
    private String content;

    private PropertyMemoItem(final Long id, final Long propertyMemoId, final Long systemMenoId,
                             final String content) {
        this.id = id;
        this.propertyMemoId = propertyMemoId;
        this.systemMenoId = systemMenoId;
        this.content = content;
    }

    public static PropertyMemoItem create(final Long propertyMemoId, final Long systemMenoId,
                                          final String content) {
        return new PropertyMemoItem(null, propertyMemoId, systemMenoId, content);
    }

    public static PropertyMemoItem reconstruct(final Long id, final Long propertyMemoId, final Long systemMenoId,
                                               final String content) {
        return new PropertyMemoItem(id, propertyMemoId, systemMenoId, content);
    }

    public void replaceContent(final String content) {
        this.content = content;
    }
}
