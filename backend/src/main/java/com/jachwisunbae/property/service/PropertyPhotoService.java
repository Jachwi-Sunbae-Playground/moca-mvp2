package com.jachwisunbae.property.service;

import com.jachwisunbae.common.exception.BusinessException;
import com.jachwisunbae.common.exception.DomainErrorCode;
import com.jachwisunbae.property.repository.PropertyPhotoRepository;
import com.jachwisunbae.property.repository.PropertyRepository;
import com.jachwisunbae.property.repository.query.PropertyPhotosQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PropertyPhotoService {
    private final PropertyRepository propertyRepository;
    private final PropertyPhotoRepository propertyPhotoRepository;

    public PropertyPhotoService(final PropertyRepository propertyRepository,
                                final PropertyPhotoRepository propertyPhotoRepository) {
        this.propertyRepository = propertyRepository;
        this.propertyPhotoRepository = propertyPhotoRepository;
    }

    public PropertyPhotosQuery find(final Long memberId, final Long propertyId) {
        findOwnedProperty(memberId, propertyId);
        return new PropertyPhotosQuery(propertyId, propertyPhotoRepository.findByPropertyId(propertyId),
                propertyPhotoRepository.findRepresentativePhotoId(propertyId).orElse(null));
    }

    @Transactional
    public void delete(final Long memberId, final Long propertyId, final Long photoId) {
        findOwnedProperty(memberId, propertyId);
        propertyPhotoRepository.findByIdAndPropertyId(photoId, propertyId)
                .orElseThrow(() -> new BusinessException(DomainErrorCode.PHOTO_NOT_FOUND,
                        "사진을 찾을 수 없습니다."));
        propertyPhotoRepository.deleteById(photoId);
        propertyPhotoRepository.ensureRepresentative(propertyId);
    }

    @Transactional
    public void designateRepresentative(final Long memberId, final Long propertyId, final Long photoId) {
        findOwnedProperty(memberId, propertyId);
        propertyPhotoRepository.findByIdAndPropertyId(photoId, propertyId)
                .orElseThrow(() -> new BusinessException(DomainErrorCode.PHOTO_NOT_FOUND,
                        "사진을 찾을 수 없습니다."));
        propertyPhotoRepository.setRepresentative(propertyId, photoId);
    }

    private void findOwnedProperty(final Long memberId, final Long propertyId) {
        if (!propertyRepository.existsByIdAndMemberId(propertyId, memberId)) {
            throw new BusinessException(DomainErrorCode.PROPERTY_NOT_FOUND, "매물을 찾을 수 없습니다.");
        }
    }
}
