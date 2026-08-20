package com.jachwisunbae.property.controller.dto.response;

public record PropertyMemoItemResponse(Long propertyMemoItemId, Long systemMemoItemId, String label,
                                       Integer displayOrder, String content) {
}
