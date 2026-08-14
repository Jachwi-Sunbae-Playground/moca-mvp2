package com.jachwisunbae.property.storage;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration(proxyBeanMethods = false)
@Profile("!test")
public class PhotoStorageConfiguration {

    /**
     * 로컬 MinIO처럼 정적 자격증명으로 접속하는 환경에서 사용한다.
     */
    @Bean(name = "photoS3Client", destroyMethod = "close")
    @Profile("!prod")
    S3Client staticCredentialsPhotoS3Client(final PhotoStorageProperties properties) {
        requireStaticCredentials(properties);
        return S3Client.builder()
                .endpointOverride(URI.create(properties.endpoint()))
                .region(Region.of(properties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                        properties.accessKey(),
                        properties.secretKey()
                )))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    /**
     * 운영에서 사용한다. EC2 인스턴스 role로 접속하므로 정적 자격증명을 두지 않는다.
     */
    @Bean(name = "photoS3Client", destroyMethod = "close")
    @Profile("prod")
    S3Client instanceRolePhotoS3Client(final PhotoStorageProperties properties) {
        return S3Client.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .build();
    }

    private void requireStaticCredentials(final PhotoStorageProperties properties) {
        if (isBlank(properties.endpoint()) || isBlank(properties.accessKey()) || isBlank(properties.secretKey())) {
            throw new IllegalStateException(
                    "정적 자격증명으로 접속하는 환경에서는 photo.storage 의 endpoint, access-key, secret-key 가 모두 필요하다."
            );
        }
    }

    private boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}
