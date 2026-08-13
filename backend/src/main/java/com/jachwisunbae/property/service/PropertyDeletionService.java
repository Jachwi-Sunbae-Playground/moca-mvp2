package com.jachwisunbae.property.service;

import com.jachwisunbae.common.exception.client.ClientException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.exception.server.ExternalServiceException;
import com.jachwisunbae.property.repository.PropertyPhotoRepository;
import com.jachwisunbae.property.storage.PhotoStorage;
import com.jachwisunbae.property.storage.PhotoStorageException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class PropertyDeletionService {

    private static final int MAX_DELETE_ATTEMPTS = 3;

    private final PropertyAccessService propertyAccessService;
    private final PropertyPhotoRepository propertyPhotoRepository;
    private final PropertyDeleteTransactionService transactionService;
    private final PhotoStorage photoStorage;

    public PropertyDeletionService(
            final PropertyAccessService propertyAccessService,
            final PropertyPhotoRepository propertyPhotoRepository,
            final PropertyDeleteTransactionService transactionService,
            final PhotoStorage photoStorage
    ) {
        this.propertyAccessService = propertyAccessService;
        this.propertyPhotoRepository = propertyPhotoRepository;
        this.transactionService = transactionService;
        this.photoStorage = photoStorage;
    }

    public void deleteProperty(final long memberId, final long propertyId) {
        final List<String> initialStorageKeys;
        try {
            propertyAccessService.requireOwned(memberId, propertyId);
            initialStorageKeys = propertyPhotoRepository.findStorageKeysOwned(memberId, propertyId);
        } catch (ClientException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ExternalServiceException(ErrorCode.PHOTO_DELETE_FAILED, exception);
        }
        List<String> pendingStorageKeys = initialStorageKeys;
        final Set<String> externallyDeletedStorageKeys = new HashSet<>();

        for (int attempt = 0; attempt < MAX_DELETE_ATTEMPTS; attempt++) {
            deleteExternalPhotos(pendingStorageKeys, externallyDeletedStorageKeys);
            final PropertyDeleteTransactionService.DeleteAttempt deleteAttempt;
            try {
                deleteAttempt = transactionService.deleteWhenPhotosRemoved(
                        memberId,
                        propertyId,
                        Set.copyOf(externallyDeletedStorageKeys)
                );
            } catch (ClientException exception) {
                throw exception;
            } catch (RuntimeException exception) {
                throw new ExternalServiceException(ErrorCode.PHOTO_DELETE_FAILED, exception);
            }
            if (deleteAttempt.deleted()) {
                return;
            }
            pendingStorageKeys = deleteAttempt.pendingStorageKeys();
        }
        throw new ExternalServiceException(ErrorCode.PHOTO_DELETE_FAILED);
    }

    private void deleteExternalPhotos(
            final List<String> storageKeys,
            final Set<String> externallyDeletedStorageKeys
    ) {
        for (final String storageKey : storageKeys) {
            try {
                photoStorage.deleteIfExists(storageKey);
                externallyDeletedStorageKeys.add(storageKey);
            } catch (PhotoStorageException exception) {
                throw new ExternalServiceException(ErrorCode.PHOTO_DELETE_FAILED, exception);
            }
        }
    }
}
