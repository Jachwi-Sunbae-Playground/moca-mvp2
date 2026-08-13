package com.jachwisunbae.property.service;

import com.jachwisunbae.common.exception.client.ResourceNotFoundException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.exception.server.DataInconsistencyException;
import com.jachwisunbae.property.repository.PropertyRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class PropertyAccessService {

    private final PropertyRepository propertyRepository;

    public PropertyAccessService(final PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    public void requireOwned(final long memberId, final long propertyId) {
        if (!propertyRepository.existsOwned(memberId, propertyId)) {
            throw new ResourceNotFoundException(ErrorCode.PROPERTY_NOT_FOUND);
        }
    }

    public void lockOwned(final long memberId, final long propertyId) {
        propertyRepository.findOwnedByIdForUpdate(memberId, propertyId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROPERTY_NOT_FOUND));
    }

    public void updateLastActivity(
            final long memberId,
            final long propertyId,
            final Instant lastActivityAt
    ) {
        if (!propertyRepository.updateLastActivity(memberId, propertyId, lastActivityAt)) {
            throw new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
