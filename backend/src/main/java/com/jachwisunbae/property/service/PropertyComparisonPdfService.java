package com.jachwisunbae.property.service;

import com.jachwisunbae.common.exception.BusinessException;
import com.jachwisunbae.common.exception.DomainErrorCode;
import com.jachwisunbae.property.controller.dto.response.PropertyChecklistOverviewResponse;
import com.jachwisunbae.property.entity.Property;
import com.jachwisunbae.property.repository.PropertyRepository;
import com.jachwisunbae.property.storage.PhotoContent;
import java.util.HashSet;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PropertyComparisonPdfService {
    private static final int MIN_PROPERTIES = 2;
    private static final int MAX_PROPERTIES = 5;

    private final PropertyRepository propertyRepository;
    private final PropertyPhotoService propertyPhotoService;
    private final PropertyMemoService propertyMemoService;
    private final PropertyChecklistService propertyChecklistService;
    private final PropertyComparisonPhotoOptimizer photoOptimizer;
    private final PropertyComparisonPdfRenderer renderer;

    public PropertyComparisonPdfService(final PropertyRepository propertyRepository,
                                        final PropertyPhotoService propertyPhotoService,
                                        final PropertyMemoService propertyMemoService,
                                        final PropertyChecklistService propertyChecklistService,
                                        final PropertyComparisonPhotoOptimizer photoOptimizer,
                                        final PropertyComparisonPdfRenderer renderer) {
        this.propertyRepository = propertyRepository;
        this.propertyPhotoService = propertyPhotoService;
        this.propertyMemoService = propertyMemoService;
        this.propertyChecklistService = propertyChecklistService;
        this.photoOptimizer = photoOptimizer;
        this.renderer = renderer;
    }

    public byte[] export(final Long memberId, final List<Long> propertyIds) {
        validateSelection(propertyIds);
        List<PropertyComparisonRecord> records = propertyIds.stream()
                .map(propertyId -> collect(memberId, propertyId))
                .toList();
        return renderer.render(records);
    }

    private PropertyComparisonRecord collect(final Long memberId, final Long propertyId) {
        Property property = propertyRepository.findByIdAndMemberId(propertyId, memberId)
                .orElseThrow(() -> new BusinessException(DomainErrorCode.PROPERTY_NOT_FOUND,
                        "비교할 매물을 찾을 수 없습니다."));
        var photoQuery = propertyPhotoService.find(memberId, propertyId);
        List<PropertyComparisonRecord.Photo> photos = photoQuery.photos().stream()
                .map(photo -> {
                    PhotoContent content = propertyPhotoService.findContent(memberId, propertyId, photo.getId());
                    return new PropertyComparisonRecord.Photo(photo.getId(), photoOptimizer.optimize(content.bytes()),
                            "image/jpeg",
                            photo.getId().equals(photoQuery.representativePhotoId()));
                })
                .toList();
        var overview = PropertyChecklistOverviewResponse.from(propertyId,
                propertyChecklistService.findOverview(memberId, propertyId));
        List<PropertyComparisonRecord.Stage> stages = overview.stages().stream()
                .map(stage -> new PropertyComparisonRecord.Stage(stage,
                        stage.applied()
                                ? propertyChecklistService.findApplication(memberId, propertyId,
                                stage.propertyChecklistId())
                                : null))
                .toList();
        return new PropertyComparisonRecord(property, photos, propertyMemoService.find(memberId, propertyId), stages);
    }

    private void validateSelection(final List<Long> propertyIds) {
        if (propertyIds == null || propertyIds.size() < MIN_PROPERTIES || propertyIds.size() > MAX_PROPERTIES
                || propertyIds.stream().anyMatch(id -> id == null || id <= 0)
                || new HashSet<>(propertyIds).size() != propertyIds.size()) {
            throw new BusinessException(DomainErrorCode.PROPERTY_INPUT_INVALID,
                    "비교할 서로 다른 매물을 2개 이상 5개 이하로 선택해야 합니다.");
        }
    }
}
