package com.jachwisunbae.common;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class PhotoStorageTestConfig {

    @Bean
    public FakePhotoStorage fakePhotoStorage() {
        return new FakePhotoStorage();
    }
}
