package com.jachwisunbae.property.storage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.services.s3.S3Client;

class PhotoStorageConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PhotoStorageConfiguration.class);

    @Test
    @DisplayName("운영 프로필은 정적 자격증명 없이도 S3 client를 만든다.")
    void prodDoesNotRequireStaticCredentials() {
        runner.withPropertyValues("spring.profiles.active=prod")
                .withBean(PhotoStorageProperties.class, () -> properties(null, null, null))
                .run(context -> assertThat(context).hasSingleBean(S3Client.class));
    }

    @Test
    @DisplayName("운영이 아닌 프로필은 정적 자격증명으로 S3 client를 만든다.")
    void nonProdUsesStaticCredentials() {
        runner.withPropertyValues("spring.profiles.active=local")
                .withBean(PhotoStorageProperties.class, () -> properties("http://localhost:9000", "ak", "sk"))
                .run(context -> assertThat(context).hasSingleBean(S3Client.class));
    }

    @Test
    @DisplayName("운영이 아닌 프로필에서 정적 자격증명이 없으면 기동에 실패한다.")
    void nonProdFailsWithoutStaticCredentials() {
        runner.withPropertyValues("spring.profiles.active=local")
                .withBean(PhotoStorageProperties.class, () -> properties(null, null, null))
                .run(context -> assertThat(context).hasFailed());
    }

    private PhotoStorageProperties properties(
            final String endpoint,
            final String accessKey,
            final String secretKey
    ) {
        return new PhotoStorageProperties(
                "ap-northeast-2",
                "bucket",
                "jachwi-sunbae/",
                endpoint,
                accessKey,
                secretKey
        );
    }
}
