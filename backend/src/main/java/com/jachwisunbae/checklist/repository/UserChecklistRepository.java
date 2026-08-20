package com.jachwisunbae.checklist.repository;

import com.jachwisunbae.checklist.entity.UserChecklist;
import com.jachwisunbae.checklist.entity.UserChecklistItem;

import com.jachwisunbae.checklist.repository.query.UserChecklistItemDetail;
import java.util.List;
import java.util.Optional;
import com.jachwisunbae.checklist.type.CheckStage;

public interface UserChecklistRepository {

    UserChecklist save(UserChecklist checklist);

    void saveItems(long checklistId, List<UserChecklistItem> items);

    Optional<UserChecklist> findByIdAndMemberId(long checklistId, long memberId);

    boolean existsByIdAndMemberId(long checklistId, long memberId);

    Optional<UserChecklist> findByIdAndMemberIdForUpdate(long checklistId, long memberId);

    List<UserChecklist> findByMemberId(long memberId, CheckStage stage);

    List<UserChecklistItem> findItems(long checklistId);

    List<UserChecklistItemDetail> findItemDetails(long checklistId);

    void updateName(long checklistId, String name);

    void deleteItems(long checklistId);

    void delete(long checklistId);
}
