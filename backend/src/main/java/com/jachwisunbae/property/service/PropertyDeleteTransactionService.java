package com.jachwisunbae.property.service;

import com.jachwisunbae.common.exception.client.ResourceNotFoundException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.exception.server.DataInconsistencyException;
import com.jachwisunbae.property.repository.PropertyPhotoRepository;
import com.jachwisunbae.property.repository.PropertyRepository;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PropertyDeleteTransactionService {

    private final PropertyRepository propertyRepository;
    private final PropertyPhotoRepository propertyPhotoRepository;

    public PropertyDeleteTransactionService(
            final PropertyRepository propertyRepository,
            final PropertyPhotoRepository propertyPhotoRepository
    ) {
        this.propertyRepository = propertyRepository;
        this.propertyPhotoRepository = propertyPhotoRepository;
    }

    @Transactional(timeout = 30)
    public DeleteAttempt deleteWhenPhotosRemoved(
            final long memberId,
            final long propertyId,
            final Set<String> externallyDeletedStorageKeys
    ) {
        propertyRepository.findOwnedByIdForUpdate(memberId, propertyId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROPERTY_NOT_FOUND));

        final List<String> currentStorageKeys = propertyPhotoRepository.findStorageKeysOwned(memberId, propertyId);
        final List<String> pendingStorageKeys = currentStorageKeys.stream()
                .filter(storageKey -> !externallyDeletedStorageKeys.contains(storageKey))
                .toList();
        if (!pendingStorageKeys.isEmpty()) {
            return DeleteAttempt.retry(pendingStorageKeys);
        }
        if (!propertyRepository.deleteOwned(memberId, propertyId)) {
            throw new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return DeleteAttempt.success();
    }

    public record DeleteAttempt(boolean deleted, List<String> pendingStorageKeys) {

        public DeleteAttempt {
            pendingStorageKeys = List.copyOf(pendingStorageKeys);
        }

        static DeleteAttempt success() {
            return new DeleteAttempt(true, List.of());
        }

        static DeleteAttempt retry(final List<String> pendingStorageKeys) {
            return new DeleteAttempt(false, pendingStorageKeys);
        }
    }
}
