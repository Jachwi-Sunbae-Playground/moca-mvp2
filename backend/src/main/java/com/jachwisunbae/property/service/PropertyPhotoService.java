package com.jachwisunbae.property.service;

import com.jachwisunbae.common.exception.BusinessException;
import com.jachwisunbae.common.exception.DomainErrorCode;
import com.jachwisunbae.property.entity.PropertyPhoto;
import com.jachwisunbae.property.repository.PropertyPhotoRepository;
import com.jachwisunbae.property.repository.PropertyRepository;
import com.jachwisunbae.property.repository.query.PropertyPhotosQuery;
import com.jachwisunbae.property.storage.PhotoContent;
import com.jachwisunbae.property.storage.PhotoStorage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class PropertyPhotoService {
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> SUPPORTED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final PropertyRepository propertyRepository;
    private final PropertyPhotoRepository propertyPhotoRepository;
    private final PhotoStorage photoStorage;
    private final Clock clock;
    private final String keyPrefix;

    public PropertyPhotoService(final PropertyRepository propertyRepository,
                                final PropertyPhotoRepository propertyPhotoRepository,
                                final PhotoStorage photoStorage,
                                final Clock clock,
                                @Value("${photo.storage.key-prefix:}") String keyPrefix) {
        this.propertyRepository = propertyRepository;
        this.propertyPhotoRepository = propertyPhotoRepository;
        this.photoStorage = photoStorage;
        this.clock = clock;
        this.keyPrefix = normalizePrefix(keyPrefix);
    }

    public PropertyPhotosQuery find(final Long memberId, final Long propertyId) {
        findOwnedProperty(memberId, propertyId);
        return new PropertyPhotosQuery(propertyId, propertyPhotoRepository.findByPropertyId(propertyId),
                propertyPhotoRepository.findRepresentativePhotoId(propertyId).orElse(null));
    }

    @Transactional
    public PropertyPhoto upload(final Long memberId, final Long propertyId, final MultipartFile file) {
        propertyRepository.findByIdAndMemberIdForUpdate(propertyId, memberId)
                .orElseThrow(() -> new BusinessException(DomainErrorCode.PROPERTY_NOT_FOUND,
                        "매물을 찾을 수 없습니다."));
        if (propertyPhotoRepository.countByPropertyId(propertyId) >= 30) {
            throw new BusinessException(DomainErrorCode.PHOTO_LIMIT_EXCEEDED,
                    "매물당 사진은 30장까지 업로드할 수 있습니다.");
        }
        byte[] bytes = readBytes(file);
        String contentType = validateContentType(file.getContentType());
        validateSize(bytes.length);
        validateImageFormat(bytes, contentType);
        String storageKey = createStorageKey(memberId, propertyId, contentType);
        photoStorage.upload(storageKey, bytes, contentType);
        try {
            PropertyPhoto saved = propertyPhotoRepository.save(memberId,
                    PropertyPhoto.create(propertyId, storageKey, contentType, (long) bytes.length,
                            LocalDateTime.now(clock)), checksum(bytes));
            propertyPhotoRepository.ensureRepresentative(propertyId);
            propertyRepository.touch(propertyId, LocalDateTime.now(clock));
            return saved;
        } catch (RuntimeException exception) {
            try {
                photoStorage.delete(storageKey);
            } catch (RuntimeException compensationFailure) {
                exception.addSuppressed(compensationFailure);
            }
            throw exception;
        }
    }

    public PhotoContent findContent(final Long memberId, final Long propertyId, final Long photoId) {
        findOwnedProperty(memberId, propertyId);
        PropertyPhoto photo = findPhoto(propertyId, photoId);
        return new PhotoContent(photoStorage.download(photo.getStorageKey()), photo.getContentType());
    }

    @Transactional
    public void delete(final Long memberId, final Long propertyId, final Long photoId) {
        findOwnedProperty(memberId, propertyId);
        PropertyPhoto photo = findPhoto(propertyId, photoId);
        photoStorage.delete(photo.getStorageKey());
        propertyPhotoRepository.deleteById(photoId);
        propertyPhotoRepository.ensureRepresentative(propertyId);
        propertyRepository.touch(propertyId, LocalDateTime.now(clock));
    }

    @Transactional
    public void designateRepresentative(final Long memberId, final Long propertyId, final Long photoId) {
        findOwnedProperty(memberId, propertyId);
        propertyPhotoRepository.findByIdAndPropertyId(photoId, propertyId)
                .orElseThrow(() -> new BusinessException(DomainErrorCode.PHOTO_NOT_FOUND,
                        "사진을 찾을 수 없습니다."));
        propertyPhotoRepository.setRepresentative(propertyId, photoId);
        propertyRepository.touch(propertyId, LocalDateTime.now(clock));
    }

    private void findOwnedProperty(final Long memberId, final Long propertyId) {
        if (!propertyRepository.existsByIdAndMemberId(propertyId, memberId)) {
            throw new BusinessException(DomainErrorCode.PROPERTY_NOT_FOUND, "매물을 찾을 수 없습니다.");
        }
    }

    private PropertyPhoto findPhoto(final Long propertyId, final Long photoId) {
        return propertyPhotoRepository.findByIdAndPropertyId(photoId, propertyId)
                .orElseThrow(() -> new BusinessException(DomainErrorCode.PHOTO_NOT_FOUND,
                        "사진을 찾을 수 없습니다."));
    }

    private byte[] readBytes(final MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new BusinessException(DomainErrorCode.PHOTO_STORAGE_FAILURE,
                    "업로드 사진을 읽을 수 없습니다.", exception);
        }
    }

    private String validateContentType(final String value) {
        String contentType = value == null ? "" : value.toLowerCase(Locale.ROOT);
        if (!SUPPORTED_TYPES.contains(contentType)) {
            throw new BusinessException(DomainErrorCode.PHOTO_CONTENT_TYPE_UNSUPPORTED,
                    "JPEG, PNG, WebP 사진만 업로드할 수 있습니다.");
        }
        return contentType;
    }

    private void validateSize(final int size) {
        if (size <= 0 || size > MAX_SIZE_BYTES) {
            throw new BusinessException(DomainErrorCode.PHOTO_SIZE_EXCEEDED,
                    "사진은 1바이트 이상 5MiB 이하여야 합니다.");
        }
    }

    private void validateImageFormat(final byte[] bytes, final String contentType) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = input == null ? java.util.Collections.emptyIterator()
                    : ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw invalidPhotoFormat();
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                boolean matches = switch (contentType) {
                    case "image/jpeg" -> format.equals("jpeg") || format.equals("jpg");
                    case "image/png" -> format.equals("png");
                    case "image/webp" -> format.equals("webp");
                    default -> false;
                };
                if (!matches) {
                    throw invalidPhotoFormat();
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new BusinessException(DomainErrorCode.PHOTO_CONTENT_TYPE_UNSUPPORTED,
                    "손상되었거나 지원하지 않는 사진입니다.", exception);
        }
    }

    private BusinessException invalidPhotoFormat() {
        return new BusinessException(DomainErrorCode.PHOTO_CONTENT_TYPE_UNSUPPORTED,
                "파일 내용과 사진 형식이 일치하지 않습니다.");
    }

    private String createStorageKey(final Long memberId, final Long propertyId, final String contentType) {
        String extension = switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> "";
        };
        return keyPrefix + "members/" + memberId + "/properties/" + propertyId + "/"
                + UUID.randomUUID() + extension;
    }

    private String checksum(final byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }

    private static String normalizePrefix(final String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }
        return prefix.endsWith("/") ? prefix : prefix + "/";
    }
}
