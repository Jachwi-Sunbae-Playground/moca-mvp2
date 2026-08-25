package com.jachwisunbae.checklist.service;

import com.jachwisunbae.checklist.controller.dto.request.CreateUserChecklistRequest;
import com.jachwisunbae.checklist.controller.dto.request.UpdateUserChecklistRequest;
import com.jachwisunbae.checklist.controller.dto.request.UserChecklistItemRequest;
import com.jachwisunbae.checklist.entity.SystemCheckItem;
import com.jachwisunbae.checklist.entity.UserChecklist;
import com.jachwisunbae.checklist.entity.UserChecklistItem;
import com.jachwisunbae.checklist.repository.SystemCheckItemRepository;
import com.jachwisunbae.checklist.repository.UserChecklistRepository;
import com.jachwisunbae.checklist.repository.query.UserChecklistItemDetail;
import com.jachwisunbae.checklist.type.CheckStage;
import com.jachwisunbae.common.exception.BusinessException;
import com.jachwisunbae.common.exception.DomainErrorCode;
import com.jachwisunbae.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UserChecklistService {

    private final SystemCheckItemRepository systemCheckItemRepository;
    private final UserChecklistRepository userChecklistRepository;
    private final UserChecklistValidator validator;
    private final MemberRepository memberRepository;

    public UserChecklistService(final SystemCheckItemRepository systemCheckItemRepository,
                                final UserChecklistRepository userChecklistRepository,
                                final UserChecklistValidator validator,
                                final MemberRepository memberRepository) {
        this.systemCheckItemRepository = systemCheckItemRepository;
        this.userChecklistRepository = userChecklistRepository;
        this.validator = validator;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public UserChecklist create(final Long memberId, final CreateUserChecklistRequest request) {
        validator.validateItems(request.items());
        Map<Long, SystemCheckItem> systemItems = findSystemItems(request.stage(), request.items());
        requireActive(systemItems.values().stream().toList());
        UserChecklist persistedChecklist = userChecklistRepository.save(
                UserChecklist.create(memberId, request.name(), request.stage()));
        saveChecklistItems(persistedChecklist,
                createItems(persistedChecklist.getId(), request.stage(), request.items(), systemItems));
        return persistedChecklist;
    }

    public List<UserChecklist> findAll(final Long memberId, final CheckStage stage) {
        return userChecklistRepository.findByMemberId(memberId, stage);
    }

    public UserChecklist find(final Long memberId, final long checklistId) {
        return findOwnedChecklist(memberId, checklistId);
    }

    public List<UserChecklistItem> findItems(final Long memberId, final long checklistId) {
        requireOwnedChecklist(memberId, checklistId);
        return userChecklistRepository.findItems(checklistId);
    }

    public List<UserChecklistItemDetail> findItemDetails(final Long memberId, final long checklistId) {
        requireOwnedChecklist(memberId, checklistId);
        return userChecklistRepository.findItemDetails(checklistId);
    }

    @Transactional
    public UserChecklist update(final Long memberId, final long checklistId,
                                final UpdateUserChecklistRequest request) {
        memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new BusinessException(DomainErrorCode.MEMBER_NOT_FOUND, "회원을 찾을 수 없습니다."));
        UserChecklist checklist = userChecklistRepository
                .findByIdAndMemberIdForUpdate(checklistId, memberId)
                .orElseThrow(() -> new BusinessException(DomainErrorCode.CHECKLIST_NOT_FOUND, "체크리스트를 찾을 수 없습니다."));
        validator.validateItems(request.items());
        Map<Long, SystemCheckItem> systemItems = findSystemItems(checklist.getStage(), request.items());
        requireInactiveItemsAlreadyIncluded(checklistId, systemItems.values().stream().toList());
        List<UserChecklistItem> updatedItems = createItems(
                checklistId, checklist.getStage(), request.items(), systemItems);
        checklist.rename(request.name());
        userChecklistRepository.updateName(checklistId, checklist.getName());
        userChecklistRepository.deleteItems(checklistId);
        userChecklistRepository.saveItems(checklistId, updatedItems);
        return checklist;
    }

    @Transactional
    public void delete(final Long memberId, final long checklistId) {
        requireOwnedChecklist(memberId, checklistId);
        userChecklistRepository.deleteItems(checklistId);
        userChecklistRepository.delete(checklistId);
    }

    private void saveChecklistItems(final UserChecklist checklist, final List<UserChecklistItem> items) {
        userChecklistRepository.saveItems(checklist.getId(), items);
    }

    private UserChecklist findOwnedChecklist(final Long memberId, final long checklistId) {
        Optional<UserChecklist> checklist = userChecklistRepository.findByIdAndMemberId(checklistId, memberId);
        return checklist.orElseThrow(() -> new BusinessException(DomainErrorCode.CHECKLIST_NOT_FOUND,
                "체크리스트를 찾을 수 없습니다."));
    }

    private void requireOwnedChecklist(final Long memberId, final long checklistId) {
        if (!userChecklistRepository.existsByIdAndMemberId(checklistId, memberId)) {
            throw new BusinessException(DomainErrorCode.CHECKLIST_NOT_FOUND, "체크리스트를 찾을 수 없습니다.");
        }
    }

    private Map<Long, SystemCheckItem> findSystemItems(final CheckStage stage,
                                                       final List<UserChecklistItemRequest> requests) {
        List<Long> ids = requests.stream()
                .map(UserChecklistItemRequest::systemCheckItemId)
                .filter(java.util.Objects::nonNull)
                .toList();
        List<SystemCheckItem> items = systemCheckItemRepository.findByIdsAndStageInOrder(stage, ids);
        validator.validateItemsExist(ids, items);
        return items.stream().collect(Collectors.toMap(SystemCheckItem::getId, Function.identity()));
    }

    private void requireActive(final List<SystemCheckItem> items) {
        if (items.stream().anyMatch(item -> item.getDeletedAt() != null)) {
            throw new BusinessException(DomainErrorCode.CHECKLIST_INACTIVE_ITEM_NOT_ALLOWED,
                    "비활성 시스템 항목은 새 체크리스트에 추가할 수 없습니다.");
        }
    }

    private void requireInactiveItemsAlreadyIncluded(final long checklistId, final List<SystemCheckItem> items) {
        Set<Long> existingSystemIds = userChecklistRepository.findItems(checklistId).stream()
                .map(UserChecklistItem::getSystemCheckItemId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        if (items.stream().anyMatch(item -> item.getDeletedAt() != null && !existingSystemIds.contains(item.getId()))) {
            throw new BusinessException(DomainErrorCode.CHECKLIST_INACTIVE_ITEM_NOT_ALLOWED,
                    "기존 체크리스트에 없던 비활성 시스템 항목은 추가할 수 없습니다.");
        }
    }

    private List<UserChecklistItem> createItems(final long checklistId, final CheckStage stage,
                                                final List<UserChecklistItemRequest> requests,
                                                final Map<Long, SystemCheckItem> systemItems) {
        List<UserChecklistItem> items = java.util.stream.IntStream.range(0, requests.size())
                .mapToObj(index -> createItem(checklistId, stage, requests.get(index), index + 1, systemItems))
                .toList();
        validator.validateUniqueQuestions(items);
        return items;
    }

    private UserChecklistItem createItem(final long checklistId, final CheckStage stage,
                                         final UserChecklistItemRequest request, final int displayOrder,
                                         final Map<Long, SystemCheckItem> systemItems) {
        if (request.systemCheckItemId() != null) {
            return UserChecklistItem.create(checklistId, systemItems.get(request.systemCheckItemId()), displayOrder);
        }
        return UserChecklistItem.createCustom(checklistId, stage, request.question().trim(), displayOrder);
    }
}
