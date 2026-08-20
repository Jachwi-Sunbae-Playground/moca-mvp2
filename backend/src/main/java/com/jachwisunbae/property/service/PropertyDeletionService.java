package com.jachwisunbae.property.service;

import com.jachwisunbae.common.exception.BusinessException;
import com.jachwisunbae.common.exception.DomainErrorCode;
import com.jachwisunbae.property.repository.PropertyChecklistRepository;
import com.jachwisunbae.property.repository.PropertyMemoRepository;
import com.jachwisunbae.property.repository.PropertyPhotoRepository;
import com.jachwisunbae.property.repository.PropertyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PropertyDeletionService {
    private final PropertyRepository propertyRepository;
    private final PropertyMemoRepository propertyMemoRepository;
    private final PropertyChecklistRepository propertyChecklistRepository;
    private final PropertyPhotoRepository propertyPhotoRepository;

    public PropertyDeletionService(final PropertyRepository propertyRepository,
                                   final PropertyMemoRepository propertyMemoRepository,
                                   final PropertyChecklistRepository propertyChecklistRepository,
                                   final PropertyPhotoRepository propertyPhotoRepository) {
        this.propertyRepository = propertyRepository;
        this.propertyMemoRepository = propertyMemoRepository;
        this.propertyChecklistRepository = propertyChecklistRepository;
        this.propertyPhotoRepository = propertyPhotoRepository;
    }

    @Transactional
    public void delete(final Long memberId, final Long propertyId) {
        propertyRepository.findByIdAndMemberIdForUpdate(propertyId, memberId)
                .orElseThrow(() -> new BusinessException(DomainErrorCode.PROPERTY_NOT_FOUND,
                        "매물을 찾을 수 없습니다."));
        propertyMemoRepository.deleteByPropertyId(propertyId);
        propertyChecklistRepository.deleteByPropertyId(propertyId);
        propertyPhotoRepository.deleteByPropertyId(propertyId);
        propertyRepository.deleteById(propertyId);
    }
}
