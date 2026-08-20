package com.jachwisunbae.property.controller.dto.response;

import com.jachwisunbae.property.repository.query.PropertyMemoRow;
import java.util.List;

public record PropertyMemoResponse(Long propertyId, List<PropertyMemoItemResponse> items, String freeMemo) {
    public static PropertyMemoResponse from(final List<PropertyMemoRow> rows) {
        if (rows.isEmpty()) {
            return new PropertyMemoResponse(null, List.of(), "");
        }
        PropertyMemoRow first = rows.get(0);
        List<PropertyMemoItemResponse> items = rows.stream()
                .map(row -> new PropertyMemoItemResponse(row.propertyMemoItemId(), row.systemMemoItemId(), row.label(),
                        row.displayOrder(), row.content()))
                .toList();
        return new PropertyMemoResponse(first.propertyId(), items,
                first.freeMemo() == null ? "" : first.freeMemo());
    }
}
