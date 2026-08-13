package com.jachwisunbae.checklist.service;

import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.checklist.repository.ChecklistQueryRepository;
import com.jachwisunbae.checklist.service.dto.result.ChecklistDetailResult;
import com.jachwisunbae.checklist.service.dto.result.ChecklistSummaryResult;
import com.jachwisunbae.common.exception.client.ResourceNotFoundException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.page.PageQuery;
import com.jachwisunbae.common.page.PageResult;
import org.springframework.stereotype.Service;

@Service
public class ChecklistQueryService {

    private final ChecklistQueryRepository checklistQueryRepository;

    public ChecklistQueryService(final ChecklistQueryRepository checklistQueryRepository) {
        this.checklistQueryRepository = checklistQueryRepository;
    }

    public PageResult<ChecklistSummaryResult> getChecklists(
            final long memberId,
            final CheckStage stage,
            final PageQuery pageQuery
    ) {
        final var content = checklistQueryRepository.findAllOwned(memberId, stage, pageQuery);
        final long totalElements = checklistQueryRepository.countAllOwned(memberId, stage);
        return PageResult.of(content, pageQuery, totalElements);
    }

    public ChecklistDetailResult getChecklist(final long memberId, final long checklistId) {
        return checklistQueryRepository.findOwnedDetail(memberId, checklistId)
                .orElseThrow(this::checklistNotFound);
    }

    private ResourceNotFoundException checklistNotFound() {
        return new ResourceNotFoundException(ErrorCode.CHECKLIST_NOT_FOUND);
    }
}
