package com.jachwisunbae.property.entity;

import lombok.Getter;
import com.jachwisunbae.common.exception.DomainErrorCode;
import com.jachwisunbae.common.validation.DomainPreconditions;

@Getter
public class PropertyMemoItem {

    private final Long id;
    private final Long propertyMemoId;
    private final Long systemMemoItemId;
    private final String label;
    private final Integer displayOrder;
    private String content;

    private PropertyMemoItem(final Long id, final Long propertyMemoId, final Long systemMemoItemId,
                             final String label, final Integer displayOrder, final String content) {
        this.id = id;
        this.propertyMemoId = propertyMemoId;
        this.systemMemoItemId = systemMemoItemId;
        this.label = label;
        this.displayOrder = displayOrder;
        this.content = content;
    }

    public static PropertyMemoItem create(final Long propertyMemoId, final Long systemMemoItemId,
                                          final String label, final Integer displayOrder, final String content) {
        return new PropertyMemoItem(null, validateId(propertyMemoId), validateId(systemMemoItemId), label,
                displayOrder, validateContent(content));
    }

    public static PropertyMemoItem reconstruct(final Long id, final Long propertyMemoId, final Long systemMemoItemId,
                                               final String label, final Integer displayOrder, final String content) {
        return new PropertyMemoItem(id, validateId(propertyMemoId), validateId(systemMemoItemId), label,
                displayOrder, validateContent(content));
    }

    public void replaceContent(final String content) {
        this.content = validateContent(content);
    }

    private static Long validateId(final Long id) {
        return DomainPreconditions.requireNonNull(id, DomainErrorCode.PROPERTY_MEMO_INVALID,
                "메모 항목 ID는 필수입니다.");
    }

    private static String validateContent(final String content) {
        String value = defaultContent(content);
        DomainPreconditions.require(value.length() <= 100, DomainErrorCode.PROPERTY_MEMO_INVALID,
                "기본 메모 항목 내용은 100자 이하여야 합니다.");
        return value;
    }

    private static String defaultContent(final String content) {
        if (content == null) {
            return "";
        }
        return content;
    }
}
