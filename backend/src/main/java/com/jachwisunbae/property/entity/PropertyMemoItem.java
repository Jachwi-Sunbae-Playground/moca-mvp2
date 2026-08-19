package com.jachwisunbae.property.entity;

import lombok.Getter;
import com.jachwisunbae.common.exception.DomainErrorCode;
import com.jachwisunbae.common.validation.DomainPreconditions;

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
        return new PropertyMemoItem(null, validateId(propertyMemoId), validateId(systemMenoId), validateContent(content));
    }

    public static PropertyMemoItem reconstruct(final Long id, final Long propertyMemoId, final Long systemMenoId,
                                               final String content) {
        return new PropertyMemoItem(id, validateId(propertyMemoId), validateId(systemMenoId), validateContent(content));
    }

    public void replaceContent(final String content) {
        this.content = validateContent(content);
    }

    private static Long validateId(final Long id) {
        return DomainPreconditions.requireNonNull(id, DomainErrorCode.PROPERTY_MEMO_INVALID,
                "메모 항목 ID는 필수입니다.");
    }

    private static String validateContent(final String content) {
        String value = content == null ? "" : content;
        DomainPreconditions.require(value.length() <= 100, DomainErrorCode.PROPERTY_MEMO_INVALID,
                "기본 메모 항목 내용은 100자 이하여야 합니다.");
        return value;
    }
}
