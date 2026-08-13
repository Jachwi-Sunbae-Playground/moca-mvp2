package com.jachwisunbae.checklist.service;

import com.jachwisunbae.checklist.repository.ChecklistRepository;
import com.jachwisunbae.checklist.repository.ChecklistRootProjection;
import com.jachwisunbae.checklist.service.dto.result.ChecklistReferenceResult;
import com.jachwisunbae.common.exception.client.ResourceNotFoundException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChecklistReferenceQueryService {

    private final ChecklistRepository checklistRepository;

    public ChecklistReferenceQueryService(final ChecklistRepository checklistRepository) {
        this.checklistRepository = checklistRepository;
    }

    @Transactional(timeout = 30)
    public ChecklistReferenceResult getOwnedForUpdate(final long memberId, final long checklistId) {
        final ChecklistRootProjection root = checklistRepository.findOwnedForUpdate(memberId, checklistId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CHECKLIST_NOT_FOUND));
        return new ChecklistReferenceResult(
                root.checklistId(),
                root.name().value(),
                root.stage(),
                checklistRepository.countItems(checklistId)
        );
    }
}
