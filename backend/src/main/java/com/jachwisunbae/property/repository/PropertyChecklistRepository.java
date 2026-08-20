package com.jachwisunbae.property.repository;

import com.jachwisunbae.checklist.entity.UserChecklistItem;
import com.jachwisunbae.checklist.type.CheckStage;
import com.jachwisunbae.property.repository.query.PropertyChecklistApplicationQuery;
import java.util.List;

public interface PropertyChecklistRepository {
    void deleteByPropertyId(long propertyId);

    PropertyChecklistApplicationQuery replace(long propertyId, long sourceChecklistId, String checklistName,
                                              CheckStage stage, List<UserChecklistItem> items);
}
