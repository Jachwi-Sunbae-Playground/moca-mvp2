package com.jachwisunbae.property.controller.dto.response;

import com.jachwisunbae.property.repository.query.PropertyMemoQuery;
import java.util.List;

public record PropertyMemoResponse(Long propertyId, List<PropertyMemoItemResponse> items, String freeMemo) {
    public static PropertyMemoResponse from(final PropertyMemoQuery row) {
        List<PropertyMemoItemResponse> items = row.items().stream()
                .map(item -> new PropertyMemoItemResponse(item.propertyMemoItemId(), item.systemMemoItemId(),
                        item.label(), item.displayOrder(), item.content()))
                .toList();
        return new PropertyMemoResponse(row.propertyId(), items,
                row.freeMemo() == null ? "" : row.freeMemo());
    }
}
