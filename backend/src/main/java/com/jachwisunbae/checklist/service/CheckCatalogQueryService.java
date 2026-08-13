package com.jachwisunbae.checklist.service;

import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.checklist.domain.ChecklistPresetType;
import com.jachwisunbae.checklist.repository.CheckItemRepository;
import com.jachwisunbae.checklist.repository.ChecklistPresetRepository;
import com.jachwisunbae.checklist.service.dto.command.CheckItemSearchCondition;
import com.jachwisunbae.checklist.service.dto.result.CheckItemResult;
import com.jachwisunbae.checklist.service.dto.result.ChecklistPresetResult;
import com.jachwisunbae.common.exception.client.ResourceNotFoundException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.page.PageResult;
import org.springframework.stereotype.Service;

@Service
public class CheckCatalogQueryService {

    private final CheckItemRepository checkItemRepository;
    private final ChecklistPresetRepository checklistPresetRepository;

    public CheckCatalogQueryService(
            final CheckItemRepository checkItemRepository,
            final ChecklistPresetRepository checklistPresetRepository
    ) {
        this.checkItemRepository = checkItemRepository;
        this.checklistPresetRepository = checklistPresetRepository;
    }

    public PageResult<CheckItemResult> searchCheckItems(final CheckItemSearchCondition condition) {
        final var items = checkItemRepository.findAllActive(
                condition.stage(),
                condition.query(),
                condition.pageQuery()
        ).stream().map(CheckItemResult::from).toList();
        final long totalElements = checkItemRepository.countAllActive(condition.stage(), condition.query());
        return PageResult.of(items, condition.pageQuery(), totalElements);
    }

    public ChecklistPresetResult getChecklistPreset(
            final ChecklistPresetType presetType,
            final CheckStage stage
    ) {
        final var preset = checklistPresetRepository.findActive(presetType, stage)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CHECKLIST_PRESET_NOT_FOUND));
        return new ChecklistPresetResult(preset.presetType(), preset.stage(), preset.items());
    }
}
