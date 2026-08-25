package com.jachwisunbae.property.service;

import com.jachwisunbae.checklist.entity.UserChecklist;
import com.jachwisunbae.checklist.entity.UserChecklistItem;
import com.jachwisunbae.checklist.repository.UserChecklistRepository;
import com.jachwisunbae.checklist.repository.SystemCheckItemRepository;
import com.jachwisunbae.checklist.entity.SystemCheckItem;
import com.jachwisunbae.checklist.type.CheckStage;
import com.jachwisunbae.checklist.type.CheckStatus;
import com.jachwisunbae.common.exception.BusinessException;
import com.jachwisunbae.common.exception.DomainErrorCode;
import com.jachwisunbae.property.controller.dto.request.ApplyPropertyChecklistRequest;
import com.jachwisunbae.property.controller.dto.request.UpdatePropertyChecklistMemoRequest;
import com.jachwisunbae.property.controller.dto.request.UpdatePropertyChecklistStatusRequest;
import com.jachwisunbae.property.repository.PropertyChecklistRepository;
import com.jachwisunbae.property.repository.PropertyProgressRepository;
import com.jachwisunbae.property.repository.PropertyRepository;
import com.jachwisunbae.property.repository.query.PropertyChecklistApplicationQuery;
import com.jachwisunbae.property.repository.query.PropertyChecklistItemStateQuery;
import com.jachwisunbae.property.repository.query.PropertyChecklistProgressQuery;
import com.jachwisunbae.property.repository.query.PropertyChecklistItemQuery;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class PropertyChecklistService {
    private final PropertyRepository propertyRepository;
    private final UserChecklistRepository userChecklistRepository;
    private final PropertyChecklistRepository propertyChecklistRepository;
    private final PropertyProgressRepository propertyProgressRepository;
    private final SystemCheckItemRepository systemCheckItemRepository;
    private final Clock clock;

    public PropertyChecklistService(final PropertyRepository propertyRepository,
                                     final UserChecklistRepository userChecklistRepository,
                                     final PropertyChecklistRepository propertyChecklistRepository,
                                     final PropertyProgressRepository propertyProgressRepository,
                                     final SystemCheckItemRepository systemCheckItemRepository,
                                     final Clock clock) {
        this.propertyRepository = propertyRepository;
        this.userChecklistRepository = userChecklistRepository;
        this.propertyChecklistRepository = propertyChecklistRepository;
        this.propertyProgressRepository = propertyProgressRepository;
        this.systemCheckItemRepository = systemCheckItemRepository;
        this.clock = clock;
    }

    @Transactional
    public PropertyChecklistApplicationQuery apply(final Long memberId, final Long propertyId,
                                                   final CheckStage stage,
                                                   final ApplyPropertyChecklistRequest request) {
        propertyRepository.findByIdAndMemberIdForUpdate(propertyId, memberId)
                .orElseThrow(() -> new BusinessException(DomainErrorCode.PROPERTY_NOT_FOUND,
                        "매물을 찾을 수 없습니다."));
        List<PropertyChecklistItemStateQuery> previous = propertyChecklistRepository.findCurrentItems(propertyId, stage);
        propertyChecklistRepository.deleteByPropertyAndStage(propertyId, stage);
        long propertyChecklistId;
        if (request.sourceType() == ApplyPropertyChecklistRequest.SourceType.SYSTEM_DEFAULT) {
            List<SystemCheckItem> coreItems = systemCheckItemRepository.findActiveCoreByStage(stage);
            if (coreItems.isEmpty()) {
                throw new BusinessException(DomainErrorCode.CHECKLIST_ITEMS_INVALID,
                        "현재 단계의 기본 체크 항목이 없습니다.");
            }
            propertyChecklistId = propertyChecklistRepository.save(propertyId, null, "기본체크리스트", stage);
            propertyChecklistRepository.saveItems(propertyChecklistId, createDefaultSnapshotItems(coreItems, previous));
        } else {
            if (request.checklistId() == null) {
                throw new BusinessException(DomainErrorCode.CHECKLIST_NOT_FOUND,
                        "적용할 체크리스트 ID가 필요합니다.");
            }
            UserChecklist checklist = userChecklistRepository.findByIdAndMemberIdForUpdate(request.checklistId(), memberId)
                    .orElseThrow(() -> new BusinessException(DomainErrorCode.CHECKLIST_NOT_FOUND,
                            "체크리스트를 찾을 수 없습니다."));
            if (checklist.getStage() != stage) {
                throw new BusinessException(DomainErrorCode.PROPERTY_CHECKLIST_STAGE_MISMATCH,
                        "매물 적용 단계와 체크리스트 단계가 다릅니다.");
            }
            propertyChecklistId = propertyChecklistRepository.save(
                    propertyId, checklist.getId(), checklist.getName(), stage);
            propertyChecklistRepository.saveItems(propertyChecklistId,
                    createSnapshotItems(userChecklistRepository.findItems(checklist.getId()), previous));
        }
        propertyRepository.touch(propertyId, LocalDateTime.now(clock));
        return findApplication(memberId, propertyId, propertyChecklistId);
    }

    private List<PropertyChecklistItemStateQuery> createDefaultSnapshotItems(
            final List<SystemCheckItem> sourceItems,
            final List<PropertyChecklistItemStateQuery> previousItems) {
        Map<String, PropertyChecklistItemStateQuery> previousBySource = previousItems.stream()
                .collect(Collectors.toMap(this::snapshotKey, Function.identity(), (first, ignored) -> first));
        return java.util.stream.IntStream.range(0, sourceItems.size())
                .mapToObj(index -> {
                    SystemCheckItem item = sourceItems.get(index);
                    PropertyChecklistItemStateQuery previous = previousBySource.get(snapshotKey(item.getId(), item.getQuestion()));
                    return new PropertyChecklistItemStateQuery(item.getId(), item.getQuestion(), index + 1,
                            previous == null ? CheckStatus.UNCONFIRMED : previous.status(),
                            previous == null ? "" : previous.memo());
                }).toList();
    }

    private List<PropertyChecklistItemStateQuery> createSnapshotItems(final List<UserChecklistItem> sourceItems,
                                                                       final List<PropertyChecklistItemStateQuery> previousItems) {
        Map<String, PropertyChecklistItemStateQuery> previousBySource = previousItems.stream()
                .collect(Collectors.toMap(this::snapshotKey, Function.identity(), (first, ignored) -> first));
        return sourceItems.stream()
                .map(item -> inheritState(item, previousBySource.get(
                        snapshotKey(item.getSystemCheckItemId(), item.getQuestion()))))
                .toList();
    }

    private PropertyChecklistItemStateQuery inheritState(final UserChecklistItem item,
                                                         final PropertyChecklistItemStateQuery previous) {
        if (previous == null) {
            return new PropertyChecklistItemStateQuery(item.getSystemCheckItemId(), item.getQuestion(),
                    item.getDisplayOrder(), CheckStatus.UNCONFIRMED, "");
        }
        return new PropertyChecklistItemStateQuery(item.getSystemCheckItemId(), item.getQuestion(),
                item.getDisplayOrder(), previous.status(), previous.memo());
    }

    private String snapshotKey(final PropertyChecklistItemStateQuery item) {
        return snapshotKey(item.systemCheckItemId(), item.question());
    }

    private String snapshotKey(final Long systemCheckItemId, final String question) {
        return systemCheckItemId == null ? "CUSTOM:" + question : "SYSTEM:" + systemCheckItemId;
    }

    public List<PropertyChecklistProgressQuery> findOverview(final Long memberId, final Long propertyId) {
        if (!propertyRepository.existsByIdAndMemberId(propertyId, memberId)) {
            throw new BusinessException(DomainErrorCode.PROPERTY_NOT_FOUND, "매물을 찾을 수 없습니다.");
        }
        return propertyProgressRepository.findByPropertyIdAndStage(propertyId);
    }

    public PropertyChecklistApplicationQuery findApplication(final Long memberId, final Long propertyId,
                                                              final Long propertyChecklistId) {
        return propertyChecklistRepository.findApplication(memberId, propertyId, propertyChecklistId)
                .orElseThrow(() -> new BusinessException(DomainErrorCode.PROPERTY_CHECKLIST_NOT_FOUND,
                        "매물 적용 체크리스트를 찾을 수 없습니다."));
    }

    @Transactional
    public PropertyChecklistItemQuery updateStatus(final Long memberId, final Long propertyId,
                                                   final Long propertyChecklistId, final Long itemId,
                                                   final UpdatePropertyChecklistStatusRequest request) {
        findItem(memberId, propertyId, propertyChecklistId, itemId);
        propertyChecklistRepository.updateStatus(memberId, propertyId, propertyChecklistId, itemId,
                request.status().name());
        propertyRepository.touch(propertyId, LocalDateTime.now(clock));
        return findItem(memberId, propertyId, propertyChecklistId, itemId);
    }

    @Transactional
    public PropertyChecklistItemQuery updateMemo(final Long memberId, final Long propertyId,
                                                 final Long propertyChecklistId, final Long itemId,
                                                 final UpdatePropertyChecklistMemoRequest request) {
        findItem(memberId, propertyId, propertyChecklistId, itemId);
        propertyChecklistRepository.updateMemo(memberId, propertyId, propertyChecklistId, itemId,
                request.memo());
        propertyRepository.touch(propertyId, LocalDateTime.now(clock));
        return findItem(memberId, propertyId, propertyChecklistId, itemId);
    }

    private PropertyChecklistItemQuery findItem(final Long memberId, final Long propertyId,
                                                final Long propertyChecklistId, final Long itemId) {
        return propertyChecklistRepository.findItem(memberId, propertyId, propertyChecklistId, itemId)
                .orElseThrow(this::itemNotFound);
    }

    private BusinessException itemNotFound() {
        return new BusinessException(DomainErrorCode.PROPERTY_CHECKLIST_ITEM_NOT_FOUND,
                "매물 체크 항목을 찾을 수 없습니다.");
    }
}
