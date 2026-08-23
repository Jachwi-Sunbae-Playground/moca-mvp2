package com.jachwisunbae.property.storage;

import com.jachwisunbae.common.exception.BusinessException;
import com.jachwisunbae.common.exception.DomainErrorCode;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class S3PhotoStorage implements PhotoStorage {

    private final S3Client s3Client;
    private final String bucket;

    public S3PhotoStorage(S3Client s3Client, @Value("${photo.storage.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    @Override
    public void upload(String key, byte[] bytes, String contentType) {
        try {
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .contentLength((long) bytes.length)
                    .build(), RequestBody.fromBytes(bytes));
        } catch (RuntimeException exception) {
            throw storageFailure(exception);
        }
    }

    @Override
    public byte[] download(String key) {
        try {
            return s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build()).asByteArray();
        } catch (RuntimeException exception) {
            throw storageFailure(exception);
        }
    }

    @Override
    public void delete(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (RuntimeException exception) {
            throw storageFailure(exception);
        }
    }

    private BusinessException storageFailure(RuntimeException cause) {
        return new BusinessException(DomainErrorCode.PHOTO_STORAGE_FAILURE,
                "사진 저장소 요청에 실패했습니다.", cause);
    }
}
