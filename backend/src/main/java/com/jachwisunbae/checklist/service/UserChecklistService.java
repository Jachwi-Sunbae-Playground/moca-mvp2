package com.jachwisunbae.checklist.service;

import com.jachwisunbae.checklist.controller.dto.request.CreateUserChecklistRequest;
import com.jachwisunbae.checklist.controller.dto.request.UpdateUserChecklistRequest;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

        List<Long> optionalIds = request.optionalSystemCheckItemIds();
        validator.validateItemIds(optionalIds);

        List<SystemCheckItem> coreItems = findActiveCore(request.stage());
        List<SystemCheckItem> optionalItems = systemCheckItemRepository.findByIdsInOrder(optionalIds);
        validator.validateItemsExist(optionalIds, optionalItems);
        validator.validateItemIds(orderedIds(coreItems, optionalIds));

        List<SystemCheckItem> orderedSystemItems = orderItems(coreItems, optionalItems, optionalIds);
        UserChecklist persistedChecklist = userChecklistRepository.save(
                UserChecklist.create(memberId, request.name(), request.stage()));
        saveChecklistItems(persistedChecklist, orderedSystemItems);
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
        validator.validateItemIds(request.systemCheckItemIds());

        List<SystemCheckItem> systemItems = systemCheckItemRepository
                .findByIdsInOrder(request.systemCheckItemIds());
        validator.validateItemsExist(request.systemCheckItemIds(), systemItems);
        checklist.rename(request.name());
        userChecklistRepository.updateName(checklistId, checklist.getName());
        userChecklistRepository.deleteItems(checklistId);
        userChecklistRepository.saveItems(checklistId, createItems(checklistId, systemItems));
        return checklist;
    }

    @Transactional
    public void delete(final Long memberId, final long checklistId) {
        requireOwnedChecklist(memberId, checklistId);
        userChecklistRepository.deleteItems(checklistId);
        userChecklistRepository.delete(checklistId);
    }

    private List<SystemCheckItem> findActiveCore(final CheckStage stage) {
        return systemCheckItemRepository.findActiveCoreByStage(stage);
    }

    private void saveChecklistItems(final UserChecklist checklist, final List<SystemCheckItem> systemItems) {
        userChecklistRepository.saveItems(checklist.getId(), createItems(checklist.getId(), systemItems));
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

    private List<SystemCheckItem> orderItems(final List<SystemCheckItem> coreItems,
                                             final List<SystemCheckItem> optionalItems,
                                             final List<Long> optionalIds) {
        Map<Long, SystemCheckItem> optionalById = optionalItems.stream()
                .collect(Collectors.toMap(SystemCheckItem::getId, Function.identity()));
        List<SystemCheckItem> ordered = new ArrayList<>(coreItems);
        optionalIds.forEach(id -> ordered.add(optionalById.get(id)));
        return ordered;
    }

    private List<Long> orderedIds(final List<SystemCheckItem> coreItems, final List<Long> optionalIds) {
        List<Long> ids = new ArrayList<>(coreItems.stream().map(SystemCheckItem::getId).toList());
        ids.addAll(optionalIds);
        return ids;
    }

    private List<UserChecklistItem> createItems(final long checklistId,
                                                final List<SystemCheckItem> systemItems) {
        List<UserChecklistItem> items = new ArrayList<>();
        for (int index = 0; index < systemItems.size(); index++) {
            items.add(UserChecklistItem.create(checklistId, systemItems.get(index), index + 1));
        }
        return items;
    }
}
