package com.jachwisunbae.property.service;

import com.jachwisunbae.common.exception.client.ResourceNotFoundException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.page.PageResult;
import com.jachwisunbae.property.repository.ActiveChecklistRepository;
import com.jachwisunbae.property.repository.PropertyPhotoRepository;
import com.jachwisunbae.property.repository.PropertyQueryRepository;
import com.jachwisunbae.property.service.dto.command.PropertySearchCondition;
import com.jachwisunbae.property.service.dto.result.ActiveChecklistResult;
import com.jachwisunbae.property.service.dto.result.PropertyDetailResult;
import com.jachwisunbae.property.service.dto.result.PropertyPhotoResult;
import com.jachwisunbae.property.service.dto.result.PropertySummaryResult;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PropertyQueryService {

    private final PropertyQueryRepository propertyQueryRepository;
    private final PropertyPhotoRepository propertyPhotoRepository;
    private final ActiveChecklistRepository activeChecklistRepository;

    public PropertyQueryService(
            final PropertyQueryRepository propertyQueryRepository,
            final PropertyPhotoRepository propertyPhotoRepository,
            final ActiveChecklistRepository activeChecklistRepository
    ) {
        this.propertyQueryRepository = propertyQueryRepository;
        this.propertyPhotoRepository = propertyPhotoRepository;
        this.activeChecklistRepository = activeChecklistRepository;
    }

    @Transactional(readOnly = true, timeout = 30)
    public PageResult<PropertySummaryResult> getProperties(
            final long memberId,
            final PropertySearchCondition condition
    ) {
        final long totalElements = propertyQueryRepository.countAllOwned(memberId, condition.query());
        final List<PropertySummaryResult> content = totalElements == 0
                ? List.of()
                : propertyQueryRepository.findAllOwned(memberId, condition.query(), condition.pageQuery())
                        .stream()
                        .map(PropertySummaryResult::from)
                        .toList();
        return PageResult.of(content, condition.pageQuery(), totalElements);
    }

    @Transactional(readOnly = true, timeout = 30)
    public PropertyDetailResult getProperty(final long memberId, final long propertyId) {
        final var projection = propertyQueryRepository.findOwnedDetail(memberId, propertyId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROPERTY_NOT_FOUND));
        final List<PropertyPhotoResult> photoPreview = propertyPhotoRepository.findPreviewOwned(memberId, propertyId)
                .stream()
                .map(PropertyPhotoResult::from)
                .toList();
        final var activeChecklists = activeChecklistRepository.findAllOwned(memberId, propertyId)
                .stream()
                .map(ActiveChecklistResult::from)
                .toList();
        return PropertyDetailResult.from(projection, activeChecklists, photoPreview);
    }
}
