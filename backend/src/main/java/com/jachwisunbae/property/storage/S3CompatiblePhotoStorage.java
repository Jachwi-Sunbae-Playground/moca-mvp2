package com.jachwisunbae.property.storage;

import java.io.InputStream;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
@Profile("!test")
public class S3CompatiblePhotoStorage implements PhotoStorage {

    private final S3Client s3Client;
    private final String bucket;

    public S3CompatiblePhotoStorage(
            final S3Client photoS3Client,
            final PhotoStorageProperties properties
    ) {
        this.s3Client = photoS3Client;
        this.bucket = properties.bucket();
    }

    @Override
    public void upload(final String storageKey, final byte[] content, final String contentType) {
        try {
            final PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .contentType(contentType)
                    .contentLength((long) content.length)
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(content));
        } catch (RuntimeException exception) {
            throw new PhotoStorageException(exception);
        }
    }

    @Override
    public InputStream open(final String storageKey) {
        try {
            final ResponseInputStream<GetObjectResponse> response = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .build());
            return response;
        } catch (NoSuchKeyException exception) {
            throw new PhotoStorageObjectNotFoundException(exception);
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                throw new PhotoStorageObjectNotFoundException(exception);
            }
            throw new PhotoStorageException(exception);
        } catch (RuntimeException exception) {
            throw new PhotoStorageException(exception);
        }
    }

    @Override
    public void deleteIfExists(final String storageKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .build());
        } catch (RuntimeException exception) {
            throw new PhotoStorageException(exception);
        }
    }
}
