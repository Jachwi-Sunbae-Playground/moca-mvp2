package com.jachwisunbae.property.storage;

import java.io.InputStream;

public interface PhotoStorage {

    void upload(String storageKey, byte[] content, String contentType);

    InputStream open(String storageKey);

    void deleteIfExists(String storageKey);
}
