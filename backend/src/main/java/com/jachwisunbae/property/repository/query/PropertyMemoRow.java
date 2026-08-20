package com.jachwisunbae.property.repository.query;

import java.util.List;

public record PropertyMemoRow(Long propertyId, String freeMemo, List<PropertyMemoItemRow> items) {
}
