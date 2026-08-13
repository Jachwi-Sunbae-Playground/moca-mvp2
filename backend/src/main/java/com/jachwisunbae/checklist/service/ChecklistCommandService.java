package com.jachwisunbae.checklist.service;

import com.jachwisunbae.checklist.domain.CheckItem;
import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.checklist.domain.Checklist;
import com.jachwisunbae.checklist.domain.ChecklistItem;
import com.jachwisunbae.checklist.domain.ChecklistItemOrigin;
import com.jachwisunbae.checklist.domain.ChecklistName;
import com.jachwisunbae.checklist.repository.CheckItemRepository;
import com.jachwisunbae.checklist.repository.ChecklistQueryRepository;
import com.jachwisunbae.checklist.repository.ChecklistRepository;
import com.jachwisunbae.checklist.repository.ChecklistRootProjection;
import com.jachwisunbae.checklist.service.dto.command.CreateChecklistCommand;
import com.jachwisunbae.checklist.service.dto.command.ChecklistItemCommand;
import com.jachwisunbae.checklist.service.dto.command.ChecklistRequestMode;
import com.jachwisunbae.checklist.service.dto.command.ReplaceChecklistCommand;
import com.jachwisunbae.checklist.service.dto.result.ChecklistDetailResult;
import com.jachwisunbae.common.exception.client.BusinessRuleViolationException;
import com.jachwisunbae.common.exception.client.ResourceNotFoundException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.exception.server.DataInconsistencyException;
import com.jachwisunbae.common.time.DatabaseTime;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChecklistCommandService {

    private final CheckItemRepository checkItemRepository;
    private final ChecklistRepository checklistRepository;
    private final ChecklistQueryRepository checklistQueryRepository;
    private final Clock clock;

    public ChecklistCommandService(
            final CheckItemRepository checkItemRepository,
            final ChecklistRepository checklistRepository,
            final ChecklistQueryRepository checklistQueryRepository,
            final Clock clock
    ) {
        this.checkItemRepository = checkItemRepository;
        this.checklistRepository = checklistRepository;
        this.checklistQueryRepository = checklistQueryRepository;
        this.clock = clock;
    }

    @Transactional(timeout = 30)
    public ChecklistDetailResult createChecklist(final long memberId, final CreateChecklistCommand command) {
        final List<ChecklistItem> items = resolveItems(command.stage(), command.items(), List.of(), false);
        final Instant now = DatabaseTime.normalize(clock.instant());
        final Checklist checklist = new Checklist(
                0,
                memberId,
                new ChecklistName(command.name()),
                command.stage(),
                items,
                now,
                now
        );
        final Checklist saved = checklistRepository.save(checklist);
        return findOwnedDetail(memberId, saved.id());
    }

    @Transactional(timeout = 30)
    public ChecklistDetailResult replaceChecklist(
            final long memberId,
            final long checklistId,
            final ReplaceChecklistCommand command
    ) {
        final ChecklistRootProjection root = findOwnedForUpdate(memberId, checklistId);
        final List<ChecklistItem> existingItems = checklistRepository.findItemsForUpdate(checklistId);
        if (command.mode() == ChecklistRequestMode.LEGACY
                && existingItems.stream().anyMatch(item -> item.origin() == ChecklistItemOrigin.CUSTOM)) {
            throw new BusinessRuleViolationException(ErrorCode.CHECKLIST_REQUIRES_V11_CLIENT);
        }
        final List<ChecklistItem> items = resolveItems(root.stage(), command.items(), existingItems, true);
        final Checklist changed = new Checklist(
                root.checklistId(),
                root.memberId(),
                new ChecklistName(command.name()),
                root.stage(),
                items,
                root.createdAt(),
                DatabaseTime.normalize(clock.instant())
        );
        if (!checklistRepository.updateRoot(changed)) {
            throw checklistNotFound();
        }
        checklistRepository.replaceItems(checklistId, existingItems, items);
        return findOwnedDetail(memberId, checklistId);
    }

    @Transactional(timeout = 30)
    public void deleteChecklist(final long memberId, final long checklistId) {
        findOwnedForUpdate(memberId, checklistId);
        if (!checklistRepository.deleteOwned(memberId, checklistId)) {
            throw checklistNotFound();
        }
    }

    private List<ChecklistItem> resolveItems(
            final CheckStage stage,
            final List<ChecklistItemCommand> commands,
            final List<ChecklistItem> existingItems,
            final boolean allowExistingCustom
    ) {
        if (commands == null || commands.isEmpty()) {
            throw new BusinessRuleViolationException(ErrorCode.CHECKLIST_EMPTY);
        }
        final Map<Long, ChecklistItem> existingById = new HashMap<>();
        final Map<Long, ChecklistItem> existingProvidedBySource = new HashMap<>();
        for (final ChecklistItem existing : existingItems) {
            existingById.put(existing.id(), existing);
            if (existing.origin() == ChecklistItemOrigin.PROVIDED) {
                existingProvidedBySource.put(existing.sourceCheckItemId(), existing);
            }
        }
        final Map<Long, CheckItem> providedById = validateProvidedItems(
                stage,
                commands,
                existingProvidedBySource.keySet()
        );
        final Set<Long> requestedCustomIds = new HashSet<>();
        return java.util.stream.IntStream.range(0, commands.size())
                .mapToObj(index -> resolveItem(
                        stage,
                        index + 1,
                        commands.get(index),
                        existingById,
                        existingProvidedBySource,
                        providedById,
                        requestedCustomIds,
                        allowExistingCustom
                ))
                .toList();
    }

    private Map<Long, CheckItem> validateProvidedItems(
            final CheckStage stage,
            final List<ChecklistItemCommand> commands,
            final Set<Long> existingProvidedIds
    ) {
        final List<Long> requestedIds = commands.stream()
                .filter(command -> command.origin() == ChecklistItemOrigin.PROVIDED)
                .map(ChecklistItemCommand::sourceCheckItemId)
                .toList();
        final Set<Long> uniqueIds = new HashSet<>(requestedIds);
        if (requestedIds.size() != uniqueIds.size()) {
            throw new BusinessRuleViolationException(ErrorCode.CHECKLIST_ITEM_DUPLICATED);
        }
        if (requestedIds.isEmpty()) {
            return Map.of();
        }
        final List<CheckItem> foundItems = checkItemRepository.findAllByIds(requestedIds);
        if (foundItems.size() != uniqueIds.size()) {
            throw new ResourceNotFoundException(ErrorCode.CHECK_ITEM_NOT_FOUND);
        }
        final Map<Long, CheckItem> itemById = new HashMap<>();
        foundItems.forEach(item -> itemById.put(item.id(), item));
        for (final long itemId : requestedIds) {
            final CheckItem item = itemById.get(itemId);
            if (item.stage() != stage) {
                throw new BusinessRuleViolationException(ErrorCode.CHECKLIST_ITEM_STAGE_MISMATCH);
            }
            if (!item.active() && !existingProvidedIds.contains(itemId)) {
                throw new BusinessRuleViolationException(ErrorCode.CHECK_ITEM_INACTIVE);
            }
        }
        return itemById;
    }

    private ChecklistItem resolveItem(
            final CheckStage stage,
            final int order,
            final ChecklistItemCommand command,
            final Map<Long, ChecklistItem> existingById,
            final Map<Long, ChecklistItem> existingProvidedBySource,
            final Map<Long, CheckItem> providedById,
            final Set<Long> requestedCustomIds,
            final boolean allowExistingCustom
    ) {
        if (command.origin() == ChecklistItemOrigin.PROVIDED) {
            final ChecklistItem existing = existingProvidedBySource.get(command.sourceCheckItemId());
            final long id = existing == null ? 0 : existing.id();
            return ChecklistItem.provided(id, providedById.get(command.sourceCheckItemId()).id(), stage, order);
        }
        final Long requestedId = command.checklistItemId();
        if (requestedId == null) {
            return ChecklistItem.custom(0, command.question(), stage, order);
        }
        if (!allowExistingCustom || !requestedCustomIds.add(requestedId)) {
            throw new BusinessRuleViolationException(ErrorCode.CHECKLIST_ITEM_DUPLICATED);
        }
        final ChecklistItem existing = existingById.get(requestedId);
        if (existing == null || existing.origin() != ChecklistItemOrigin.CUSTOM) {
            throw new ResourceNotFoundException(ErrorCode.CHECKLIST_ITEM_NOT_FOUND);
        }
        return ChecklistItem.custom(existing.id(), command.question(), stage, order);
    }

    private ChecklistRootProjection findOwnedForUpdate(final long memberId, final long checklistId) {
        return checklistRepository.findOwnedForUpdate(memberId, checklistId)
                .orElseThrow(this::checklistNotFound);
    }

    private ChecklistDetailResult findOwnedDetail(final long memberId, final long checklistId) {
        return checklistQueryRepository.findOwnedDetail(memberId, checklistId)
                .orElseThrow(() -> new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    private ResourceNotFoundException checklistNotFound() {
        return new ResourceNotFoundException(ErrorCode.CHECKLIST_NOT_FOUND);
    }
}
