package com.jachwisunbae.checklist.service;

import com.jachwisunbae.checklist.repository.ChecklistRepository;
import com.jachwisunbae.checklist.repository.ChecklistSnapshotItemProjection;
import com.jachwisunbae.checklist.service.dto.result.ChecklistSnapshotItemResult;
import com.jachwisunbae.checklist.service.dto.result.ChecklistSnapshotSourceResult;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ChecklistSnapshotSourceService {

    private final ChecklistRepository checklistRepository;

    public ChecklistSnapshotSourceService(final ChecklistRepository checklistRepository) {
        this.checklistRepository = checklistRepository;
    }

    public List<ChecklistSnapshotSourceResult> loadForVisit(final long memberId, final long propertyId) {
        final var roots = checklistRepository.findActiveOwnedForVisitForUpdate(memberId, propertyId);
        if (roots.isEmpty()) {
            return List.of();
        }
        final List<Long> checklistIds = roots.stream()
                .map(root -> root.checklistId())
                .toList();
        final Map<Long, List<ChecklistSnapshotItemProjection>> itemsByChecklist =
                checklistRepository.findSnapshotItems(checklistIds).stream()
                        .collect(Collectors.groupingBy(ChecklistSnapshotItemProjection::checklistId));
        return roots.stream()
                .map(root -> new ChecklistSnapshotSourceResult(
                        root.checklistId(),
                        root.name().value(),
                        root.stage(),
                        itemsByChecklist.getOrDefault(root.checklistId(), List.of()).stream()
                                .map(item -> new ChecklistSnapshotItemResult(
                                        item.checklistItemId(),
                                        item.origin(),
                                        item.sourceCheckItemId(),
                                        item.question(),
                                        item.guide(),
                                        item.order()
                                ))
                                .toList()
                ))
                .toList();
    }
}
