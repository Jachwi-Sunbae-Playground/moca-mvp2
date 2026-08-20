package com.jachwisunbae.property.repository.query;

import java.util.List;

public record PropertyMemoQuery(Long propertyId, String freeMemo, List<PropertyMemoItemQuery> items) {
}
