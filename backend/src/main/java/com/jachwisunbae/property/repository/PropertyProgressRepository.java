package com.jachwisunbae.property.repository;

import com.jachwisunbae.property.repository.query.PropertyProgressSummary;
import java.util.List;

public interface PropertyProgressRepository {
    PropertyProgressSummary findByPropertyId(long propertyId);
}
