package com.jachwisunbae.property.storage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PhotoStoragePropertiesTest {

    private static final String STORAGE_KEY = "members/1/properties/2/fixed";

    @Test
    @DisplayName("key prefix가 없으면 storage key를 그대로 사용한다.")
    void objectKeyWithoutPrefix() {
        final PhotoStorageProperties properties = properties(null);

        assertThat(properties.objectKey(STORAGE_KEY)).isEqualTo(STORAGE_KEY);
    }

    @Test
    @DisplayName("key prefix가 비어 있으면 storage key를 그대로 사용한다.")
    void objectKeyWithBlankPrefix() {
        final PhotoStorageProperties properties = properties("  ");

        assertThat(properties.objectKey(STORAGE_KEY)).isEqualTo(STORAGE_KEY);
    }

    @Test
    @DisplayName("key prefix가 있으면 storage key 앞에 붙인다.")
    void objectKeyWithPrefix() {
        final PhotoStorageProperties properties = properties("jachwi-sunbae/");

        assertThat(properties.objectKey(STORAGE_KEY)).isEqualTo("jachwi-sunbae/" + STORAGE_KEY);
    }

    @Test
    @DisplayName("key prefix가 /로 끝나지 않아도 구분자를 하나만 넣는다.")
    void objectKeyNormalizesSeparator() {
        final PhotoStorageProperties properties = properties("jachwi-sunbae");

        assertThat(properties.objectKey(STORAGE_KEY)).isEqualTo("jachwi-sunbae/" + STORAGE_KEY);
    }

    private PhotoStorageProperties properties(final String keyPrefix) {
        return new PhotoStorageProperties("ap-northeast-2", "bucket", keyPrefix, null, null, null);
    }
}
