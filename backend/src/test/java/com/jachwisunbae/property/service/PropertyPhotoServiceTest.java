package com.jachwisunbae.property.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jachwisunbae.common.TestImages;
import com.jachwisunbae.common.exception.JachwiException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.exception.server.DataInconsistencyException;
import com.jachwisunbae.property.domain.PropertyPhoto;
import com.jachwisunbae.property.repository.PropertyPhotoRepository;
import com.jachwisunbae.property.service.dto.command.UploadPhotoCommand;
import com.jachwisunbae.property.storage.PhotoStorage;
import com.jachwisunbae.property.storage.PhotoStorageException;
import com.jachwisunbae.property.storage.StorageKeyGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PropertyPhotoServiceTest {

    private static final long MEMBER_ID = 1L;
    private static final long PROPERTY_ID = 2L;
    private static final String STORAGE_KEY = "members/1/properties/2/fixed";

    private final PropertyAccessService propertyAccessService = mock(PropertyAccessService.class);
    private final PropertyPhotoRepository propertyPhotoRepository = mock(PropertyPhotoRepository.class);
    private final PropertyPhotoTransactionService transactionService = mock(PropertyPhotoTransactionService.class);
    private final StorageKeyGenerator storageKeyGenerator = mock(StorageKeyGenerator.class);
    private final PhotoStorage photoStorage = mock(PhotoStorage.class);
    private PropertyPhotoService service;

    @BeforeEach
    void setUp() {
        when(storageKeyGenerator.generate(MEMBER_ID, PROPERTY_ID)).thenReturn(STORAGE_KEY);
        service = new PropertyPhotoService(
                propertyAccessService,
                propertyPhotoRepository,
                transactionService,
                new ImageContentValidator(),
                storageKeyGenerator,
                photoStorage,
                Clock.fixed(Instant.parse("2026-08-10T00:00:00Z"), ZoneOffset.UTC)
        );
    }

    @DisplayName("외부 업로드 뒤 DB 저장이 실패하면 객체를 보상 삭제하고 업로드 실패를 반환한다")
    @Test
    void compensateWhenDatabaseSaveFails() {
        when(transactionService.saveMetadata(any(PropertyPhoto.class)))
                .thenThrow(new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR));

        assertError(
                () -> service.uploadPhoto(
                        MEMBER_ID,
                        PROPERTY_ID,
                        new UploadPhotoCommand("image/png", TestImages.png())
                ),
                ErrorCode.PHOTO_UPLOAD_FAILED
        );

        verify(photoStorage).deleteIfExists(STORAGE_KEY);
    }

    @DisplayName("보상 삭제까지 실패해도 원래 업로드 실패를 유지한다")
    @Test
    void keepOriginalFailureWhenCompensationFails() {
        when(transactionService.saveMetadata(any(PropertyPhoto.class)))
                .thenThrow(new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR));
        doThrow(new PhotoStorageException(new IllegalStateException("compensation failure")))
                .when(photoStorage)
                .deleteIfExists(STORAGE_KEY);

        assertError(
                () -> service.uploadPhoto(
                        MEMBER_ID,
                        PROPERTY_ID,
                        new UploadPhotoCommand("image/png", TestImages.png())
                ),
                ErrorCode.PHOTO_UPLOAD_FAILED
        );
    }

    @DisplayName("외부 업로드가 실패하면 DB 메타데이터 저장을 시도하지 않는다")
    @Test
    void doNotSaveMetadataWhenUploadFails() {
        doThrow(new PhotoStorageException(new IllegalStateException("upload failure")))
                .when(photoStorage)
                .upload(any(), any(), any());

        assertError(
                () -> service.uploadPhoto(
                        MEMBER_ID,
                        PROPERTY_ID,
                        new UploadPhotoCommand("image/png", TestImages.png())
                ),
                ErrorCode.PHOTO_UPLOAD_FAILED
        );

        verify(transactionService, never()).saveMetadata(any(PropertyPhoto.class));
    }

    @DisplayName("외부 객체 삭제가 실패하면 DB 메타데이터 삭제를 시도하지 않는다")
    @Test
    void doNotDeleteMetadataWhenStorageDeleteFails() {
        when(propertyPhotoRepository.findOwned(MEMBER_ID, PROPERTY_ID, 3L))
                .thenReturn(Optional.of(photo()));
        doThrow(new PhotoStorageException(new IllegalStateException("delete failure")))
                .when(photoStorage)
                .deleteIfExists(STORAGE_KEY);

        assertError(() -> service.deletePhoto(MEMBER_ID, PROPERTY_ID, 3L), ErrorCode.PHOTO_DELETE_FAILED);

        verify(transactionService, never()).deleteMetadata(anyLong(), anyLong(), anyLong(), any());
    }

    @DisplayName("외부 객체 삭제 뒤 DB 삭제가 실패하면 삭제 실패를 반환해 재요청할 수 있게 한다")
    @Test
    void returnFailureWhenMetadataDeleteFails() {
        when(propertyPhotoRepository.findOwned(MEMBER_ID, PROPERTY_ID, 3L))
                .thenReturn(Optional.of(photo()));
        doThrow(new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR))
                .when(transactionService)
                .deleteMetadata(anyLong(), anyLong(), anyLong(), any());

        assertError(() -> service.deletePhoto(MEMBER_ID, PROPERTY_ID, 3L), ErrorCode.PHOTO_DELETE_FAILED);

        verify(photoStorage).deleteIfExists(STORAGE_KEY);
    }

    @DisplayName("삭제 대상 DB 조회가 실패하면 외부 객체를 건드리지 않고 삭제 실패를 반환한다")
    @Test
    void doNotDeleteStorageWhenDatabaseLookupFails() {
        when(propertyPhotoRepository.findOwned(MEMBER_ID, PROPERTY_ID, 3L))
                .thenThrow(new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR));

        assertError(() -> service.deletePhoto(MEMBER_ID, PROPERTY_ID, 3L), ErrorCode.PHOTO_DELETE_FAILED);

        verify(photoStorage, never()).deleteIfExists(STORAGE_KEY);
    }

    private PropertyPhoto photo() {
        return new PropertyPhoto(
                3L,
                PROPERTY_ID,
                MEMBER_ID,
                STORAGE_KEY,
                "image/png",
                TestImages.png().length,
                "0".repeat(64),
                Instant.parse("2026-08-10T00:00:00Z")
        );
    }

    private void assertError(final Runnable action, final ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(JachwiException.class)
                .extracting(exception -> ((JachwiException) exception).getErrorCode())
                .isEqualTo(errorCode);
    }
}
