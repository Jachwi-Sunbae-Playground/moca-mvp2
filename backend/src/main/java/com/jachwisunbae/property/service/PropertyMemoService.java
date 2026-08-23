package com.jachwisunbae.property.service;

import com.jachwisunbae.common.exception.BusinessException;
import com.jachwisunbae.common.exception.DomainErrorCode;
import com.jachwisunbae.property.controller.dto.request.PropertyMemoItemRequest;
import com.jachwisunbae.property.controller.dto.request.UpdatePropertyMemoRequest;
import com.jachwisunbae.property.entity.PropertyMemo;
import com.jachwisunbae.property.entity.PropertyMemoItem;
import com.jachwisunbae.property.repository.PropertyMemoRepository;
import com.jachwisunbae.property.repository.PropertyRepository;
import com.jachwisunbae.property.repository.SystemMemoItemRepository;
import com.jachwisunbae.property.repository.query.PropertyMemoQuery;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class PropertyMemoService {
    private final PropertyRepository propertyRepository;
    private final PropertyMemoRepository propertyMemoRepository;
    private final SystemMemoItemRepository systemMemoItemRepository;
    private final Clock clock;

    public PropertyMemoService(final PropertyRepository propertyRepository,
                               final PropertyMemoRepository propertyMemoRepository,
                               final SystemMemoItemRepository systemMemoItemRepository,
                               final Clock clock) {
        this.propertyRepository = propertyRepository;
        this.propertyMemoRepository = propertyMemoRepository;
        this.systemMemoItemRepository = systemMemoItemRepository;
        this.clock = clock;
    }

    public PropertyMemoQuery find(final Long memberId, final Long propertyId) {
        findOwnedProperty(memberId, propertyId);
        return propertyMemoRepository.findQuery(propertyId);
    }

    @Transactional
    public PropertyMemoQuery initialize(final Long memberId, final Long propertyId) {
        findOwnedProperty(memberId, propertyId);
        if (propertyMemoRepository.findByPropertyId(propertyId).isEmpty()) {
            createMemoWithSnapshot(propertyId, "");
        }
        return propertyMemoRepository.findQuery(propertyId);
    }

    @Transactional
    public PropertyMemoQuery update(final Long memberId, final Long propertyId,
                                        final UpdatePropertyMemoRequest request) {
        findOwnedProperty(memberId, propertyId);
        PropertyMemo memo = propertyMemoRepository.findByPropertyId(propertyId)
                .orElseThrow(() -> new BusinessException(DomainErrorCode.PROPERTY_MEMO_INVALID,
                        "매물 메모를 먼저 생성해야 합니다."));
        updateMemo(memo, request.freeMemo());
        updateItems(memo.getId(), request.items());
        propertyRepository.touch(propertyId, LocalDateTime.now(clock));
        return propertyMemoRepository.findQuery(propertyId);
    }

    private void updateMemo(final PropertyMemo memo, final String freeMemo) {
        memo.replaceFreeMemo(freeMemo);
        propertyMemoRepository.update(memo);
    }

    private void createMemoWithSnapshot(final long propertyId, final String freeMemo) {
        PropertyMemo memo = propertyMemoRepository.save(PropertyMemo.create(propertyId, freeMemo));
        systemMemoItemRepository.findActive().forEach(item -> propertyMemoRepository.saveItem(
                PropertyMemoItem.create(memo.getId(), item.getId(), item.getLabel(), item.getDisplayOrder(), "")));
    }

    private void updateItems(final long propertyMemoId, final List<PropertyMemoItemRequest> requests) {
        for (PropertyMemoItemRequest request : requests) {
            int updated = propertyMemoRepository.updateItem(
                    propertyMemoId, request.systemMemoItemId(), request.content());
            if (updated != 1) {
                throw new BusinessException(DomainErrorCode.PROPERTY_MEMO_INVALID,
                        "다른 매물의 메모 항목은 저장할 수 없습니다.");
            }
        }
    }

    private void findOwnedProperty(final Long memberId, final Long propertyId) {
        if (!propertyRepository.existsByIdAndMemberId(propertyId, memberId)) {
            throw new BusinessException(DomainErrorCode.PROPERTY_NOT_FOUND, "매물을 찾을 수 없습니다.");
        }
    }
}
