package com.jachwisunbae.property.repository.query;

public record PropertyMemoItemRow(Long propertyMemoItemId, Long systemMemoItemId,
                                  String label, Integer displayOrder, String content) {
}
