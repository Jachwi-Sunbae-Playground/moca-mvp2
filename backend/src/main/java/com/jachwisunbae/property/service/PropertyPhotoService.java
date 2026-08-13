package com.jachwisunbae.property.service;

import com.jachwisunbae.common.exception.client.ClientException;
import com.jachwisunbae.common.exception.client.ResourceNotFoundException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.exception.server.ExternalServiceException;
import com.jachwisunbae.common.time.DatabaseTime;
import com.jachwisunbae.property.domain.PropertyPhoto;
import com.jachwisunbae.property.repository.PropertyPhotoRepository;
import com.jachwisunbae.property.service.ImageContentValidator.ValidatedImage;
import com.jachwisunbae.property.service.dto.command.UploadPhotoCommand;
import com.jachwisunbae.property.service.dto.result.PhotoContentResult;
import com.jachwisunbae.property.service.dto.result.PropertyPhotoListResult;
import com.jachwisunbae.property.service.dto.result.PropertyPhotoResult;
import com.jachwisunbae.property.storage.PhotoStorage;
import com.jachwisunbae.property.storage.PhotoStorageException;
import com.jachwisunbae.property.storage.PhotoStorageObjectNotFoundException;
import com.jachwisunbae.property.storage.StorageKeyGenerator;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PropertyPhotoService {

    private static final Logger log = LoggerFactory.getLogger(PropertyPhotoService.class);

    private final PropertyAccessService propertyAccessService;
    private final PropertyPhotoRepository propertyPhotoRepository;
    private final PropertyPhotoTransactionService transactionService;
    private final ImageContentValidator imageContentValidator;
    private final StorageKeyGenerator storageKeyGenerator;
    private final PhotoStorage photoStorage;
    private final Clock clock;

    public PropertyPhotoService(
            final PropertyAccessService propertyAccessService,
            final PropertyPhotoRepository propertyPhotoRepository,
            final PropertyPhotoTransactionService transactionService,
            final ImageContentValidator imageContentValidator,
            final StorageKeyGenerator storageKeyGenerator,
            final PhotoStorage photoStorage,
            final Clock clock
    ) {
        this.propertyAccessService = propertyAccessService;
        this.propertyPhotoRepository = propertyPhotoRepository;
        this.transactionService = transactionService;
        this.imageContentValidator = imageContentValidator;
        this.storageKeyGenerator = storageKeyGenerator;
        this.photoStorage = photoStorage;
        this.clock = clock;
    }

    public PropertyPhotoListResult getPhotos(final long memberId, final long propertyId) {
        propertyAccessService.requireOwned(memberId, propertyId);
        final List<PropertyPhotoResult> photos = propertyPhotoRepository.findAllOwned(memberId, propertyId)
                .stream()
                .map(PropertyPhotoResult::from)
                .toList();
        return new PropertyPhotoListResult(photos, photos.size());
    }

    public PropertyPhotoResult uploadPhoto(
            final long memberId,
            final long propertyId,
            final UploadPhotoCommand command
    ) {
        final ValidatedImage validatedImage = imageContentValidator.validate(command);
        try {
            propertyAccessService.requireOwned(memberId, propertyId);
        } catch (ClientException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ExternalServiceException(ErrorCode.PHOTO_UPLOAD_FAILED, exception);
        }
        final String storageKey = storageKeyGenerator.generate(memberId, propertyId);
        upload(storageKey, validatedImage);

        final Instant now = DatabaseTime.normalize(clock.instant());
        final PropertyPhoto photo = new PropertyPhoto(
                0,
                propertyId,
                memberId,
                storageKey,
                validatedImage.contentType(),
                validatedImage.sizeBytes(),
                validatedImage.checksumSha256(),
                now
        );
        try {
            return PropertyPhotoResult.from(transactionService.saveMetadata(photo));
        } catch (RuntimeException exception) {
            compensateUpload(storageKey, exception);
            if (exception instanceof ClientException clientException) {
                throw clientException;
            }
            throw new ExternalServiceException(ErrorCode.PHOTO_UPLOAD_FAILED, exception);
        }
    }

    public PhotoContentResult getPhotoContent(
            final long memberId,
            final long propertyId,
            final long photoId
    ) {
        final PropertyPhoto photo = findOwnedPhotoForRead(memberId, propertyId, photoId);
        final InputStream content;
        try {
            content = photoStorage.open(photo.storageKey());
        } catch (PhotoStorageObjectNotFoundException exception) {
            throw new ResourceNotFoundException(ErrorCode.PHOTO_NOT_FOUND);
        } catch (PhotoStorageException exception) {
            throw new ExternalServiceException(ErrorCode.PHOTO_READ_FAILED, exception);
        }
        return new PhotoContentResult(photo.contentType(), photo.sizeBytes(), content);
    }

    public void deletePhoto(final long memberId, final long propertyId, final long photoId) {
        final PropertyPhoto photo = findOwnedPhotoForDelete(memberId, propertyId, photoId);
        try {
            photoStorage.deleteIfExists(photo.storageKey());
        } catch (PhotoStorageException exception) {
            throw new ExternalServiceException(ErrorCode.PHOTO_DELETE_FAILED, exception);
        }
        try {
            transactionService.deleteMetadata(
                    memberId,
                    propertyId,
                    photoId,
                    DatabaseTime.normalize(clock.instant())
            );
        } catch (ClientException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ExternalServiceException(ErrorCode.PHOTO_DELETE_FAILED, exception);
        }
    }

    private void upload(final String storageKey, final ValidatedImage image) {
        try {
            photoStorage.upload(storageKey, image.content(), image.contentType());
        } catch (PhotoStorageException exception) {
            throw new ExternalServiceException(ErrorCode.PHOTO_UPLOAD_FAILED, exception);
        }
    }

    private void compensateUpload(final String storageKey, final RuntimeException uploadFailure) {
        try {
            photoStorage.deleteIfExists(storageKey);
        } catch (PhotoStorageException compensationFailure) {
            log.error(
                    "photo upload compensation failed: uploadFailure={}, compensationFailure={}",
                    uploadFailure.getClass().getSimpleName(),
                    compensationFailure.getClass().getSimpleName()
            );
        }
    }

    private PropertyPhoto findOwnedPhoto(final long memberId, final long propertyId, final long photoId) {
        return propertyPhotoRepository.findOwned(memberId, propertyId, photoId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PHOTO_NOT_FOUND));
    }

    private PropertyPhoto findOwnedPhotoForRead(
            final long memberId,
            final long propertyId,
            final long photoId
    ) {
        try {
            propertyAccessService.requireOwned(memberId, propertyId);
            return findOwnedPhoto(memberId, propertyId, photoId);
        } catch (ClientException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ExternalServiceException(ErrorCode.PHOTO_READ_FAILED, exception);
        }
    }

    private PropertyPhoto findOwnedPhotoForDelete(
            final long memberId,
            final long propertyId,
            final long photoId
    ) {
        try {
            propertyAccessService.requireOwned(memberId, propertyId);
            return findOwnedPhoto(memberId, propertyId, photoId);
        } catch (ClientException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ExternalServiceException(ErrorCode.PHOTO_DELETE_FAILED, exception);
        }
    }
}
