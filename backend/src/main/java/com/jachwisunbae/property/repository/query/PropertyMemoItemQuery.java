package com.jachwisunbae.property.repository.query;

public record PropertyMemoItemQuery(Long propertyMemoItemId, Long systemMemoItemId,
                                    String label, Integer displayOrder, String content) {
}
