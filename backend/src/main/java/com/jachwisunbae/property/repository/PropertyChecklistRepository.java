package com.jachwisunbae.property.repository;

import com.jachwisunbae.checklist.type.CheckStage;
import com.jachwisunbae.property.repository.query.PropertyChecklistApplicationQuery;
import com.jachwisunbae.property.repository.query.PropertyChecklistItemStateQuery;
import com.jachwisunbae.property.repository.query.PropertyChecklistItemQuery;
import java.util.List;
import java.util.Optional;

public interface PropertyChecklistRepository {
    void deleteByPropertyId(long propertyId);

    List<PropertyChecklistItemStateQuery> findCurrentItems(long propertyId, CheckStage stage);

    void deleteByPropertyAndStage(long propertyId, CheckStage stage);

    long save(long propertyId, Long sourceChecklistId, String checklistName, CheckStage stage);

    void saveItems(long propertyChecklistId, List<PropertyChecklistItemStateQuery> items);

    Optional<PropertyChecklistApplicationQuery> findApplication(long memberId, long propertyId,
                                                                  long propertyChecklistId);

    int updateStatus(long memberId, long propertyId, long propertyChecklistId, long itemId, String status);

    int updateMemo(long memberId, long propertyId, long propertyChecklistId, long itemId, String memo);

    Optional<PropertyChecklistItemQuery> findItem(long memberId, long propertyId,
                                                   long propertyChecklistId, long itemId);
}
