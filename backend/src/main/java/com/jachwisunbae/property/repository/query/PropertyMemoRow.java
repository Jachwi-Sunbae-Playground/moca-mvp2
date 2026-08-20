package com.jachwisunbae.property.repository.query;

public record PropertyMemoRow(Long propertyId, Long propertyMemoId, String freeMemo,
                              Long propertyMemoItemId,
                              Long systemMemoItemId, String label, Integer displayOrder,
                              String content) {
}
