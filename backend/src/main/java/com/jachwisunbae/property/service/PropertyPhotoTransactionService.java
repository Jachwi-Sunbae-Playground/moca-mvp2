package com.jachwisunbae.property.service;

import com.jachwisunbae.common.exception.client.BusinessRuleViolationException;
import com.jachwisunbae.common.exception.client.ResourceNotFoundException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.exception.server.DataInconsistencyException;
import com.jachwisunbae.property.domain.PropertyPhoto;
import com.jachwisunbae.property.repository.PropertyPhotoRepository;
import com.jachwisunbae.property.repository.PropertyRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PropertyPhotoTransactionService {

    static final int MAX_PHOTO_COUNT = 30;

    private final PropertyRepository propertyRepository;
    private final PropertyPhotoRepository propertyPhotoRepository;

    public PropertyPhotoTransactionService(
            final PropertyRepository propertyRepository,
            final PropertyPhotoRepository propertyPhotoRepository
    ) {
        this.propertyRepository = propertyRepository;
        this.propertyPhotoRepository = propertyPhotoRepository;
    }

    @Transactional(timeout = 30)
    public PropertyPhoto saveMetadata(final PropertyPhoto photo) {
        requireOwnedForUpdate(photo.memberId(), photo.propertyId());
        if (propertyPhotoRepository.countOwned(photo.memberId(), photo.propertyId()) >= MAX_PHOTO_COUNT) {
            throw new BusinessRuleViolationException(ErrorCode.PHOTO_COUNT_EXCEEDED);
        }
        final PropertyPhoto savedPhoto = propertyPhotoRepository.save(photo);
        updateLastActivity(photo.memberId(), photo.propertyId(), photo.createdAt());
        return savedPhoto;
    }

    @Transactional(timeout = 30)
    public void deleteMetadata(
            final long memberId,
            final long propertyId,
            final long photoId,
            final Instant deletedAt
    ) {
        requireOwnedForUpdate(memberId, propertyId);
        if (!propertyPhotoRepository.deleteOwned(memberId, propertyId, photoId)) {
            throw new ResourceNotFoundException(ErrorCode.PHOTO_NOT_FOUND);
        }
        updateLastActivity(memberId, propertyId, deletedAt);
    }

    private void requireOwnedForUpdate(final long memberId, final long propertyId) {
        propertyRepository.findOwnedByIdForUpdate(memberId, propertyId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROPERTY_NOT_FOUND));
    }

    private void updateLastActivity(final long memberId, final long propertyId, final Instant occurredAt) {
        if (!propertyRepository.updateLastActivity(memberId, propertyId, occurredAt)) {
            throw new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
