package com.jachwisunbae.property.service;

import com.jachwisunbae.common.exception.BusinessException;
import com.jachwisunbae.common.exception.DomainErrorCode;
import com.jachwisunbae.property.controller.dto.request.PropertyMemoItemRequest;
import com.jachwisunbae.property.controller.dto.request.UpdatePropertyMemoRequest;
import com.jachwisunbae.property.entity.PropertyMemo;
import com.jachwisunbae.property.repository.PropertyMemoRepository;
import com.jachwisunbae.property.repository.PropertyRepository;
import com.jachwisunbae.property.repository.query.PropertyMemoQuery;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PropertyMemoService {
    private final PropertyRepository propertyRepository;
    private final PropertyMemoRepository propertyMemoRepository;
    private final PropertyMemoInitializer propertyMemoInitializer;

    public PropertyMemoService(final PropertyRepository propertyRepository,
                               final PropertyMemoRepository propertyMemoRepository,
                               final PropertyMemoInitializer propertyMemoInitializer) {
        this.propertyRepository = propertyRepository;
        this.propertyMemoRepository = propertyMemoRepository;
        this.propertyMemoInitializer = propertyMemoInitializer;
    }

    public PropertyMemoQuery find(final Long memberId, final Long propertyId) {
        findOwnedProperty(memberId, propertyId);
        return propertyMemoRepository.findQuery(propertyId);
    }

    @Transactional
    public PropertyMemoQuery initialize(final Long memberId, final Long propertyId) {
        findOwnedPropertyForUpdate(memberId, propertyId);
        propertyMemoInitializer.initialize(propertyId);
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
        updateItems(request.items());
        return propertyMemoRepository.findQuery(propertyId);
    }

    private void updateMemo(final PropertyMemo memo, final String freeMemo) {
        memo.replaceFreeMemo(freeMemo);
        propertyMemoRepository.update(memo);
    }

    private void updateItems(final List<PropertyMemoItemRequest> requests) {
        for (PropertyMemoItemRequest request : requests) {
            propertyMemoRepository.updateItem(request.propertyMemoItemId(), request.content());
        }
    }

    private void findOwnedProperty(final Long memberId, final Long propertyId) {
        if (!propertyRepository.existsByIdAndMemberId(propertyId, memberId)) {
            throw new BusinessException(DomainErrorCode.PROPERTY_NOT_FOUND, "매물을 찾을 수 없습니다.");
        }
    }

    private void findOwnedPropertyForUpdate(final Long memberId, final Long propertyId) {
        propertyRepository.findByIdAndMemberIdForUpdate(propertyId, memberId)
                .orElseThrow(() -> new BusinessException(
                        DomainErrorCode.PROPERTY_NOT_FOUND, "매물을 찾을 수 없습니다."));
    }
}
