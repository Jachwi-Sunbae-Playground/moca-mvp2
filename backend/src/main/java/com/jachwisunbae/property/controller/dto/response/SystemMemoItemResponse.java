package com.jachwisunbae.property.controller.dto.response;

import com.jachwisunbae.property.entity.SystemMemoItem;

public record SystemMemoItemResponse(Long id, String label, Integer displayOrder) {
    public static SystemMemoItemResponse from(final SystemMemoItem item) {
        return new SystemMemoItemResponse(item.getId(), item.getLabel(), item.getDisplayOrder());
    }
}
