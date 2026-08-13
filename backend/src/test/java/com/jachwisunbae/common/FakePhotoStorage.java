package com.jachwisunbae.common;

import com.jachwisunbae.property.storage.PhotoStorage;
import com.jachwisunbae.property.storage.PhotoStorageException;
import com.jachwisunbae.property.storage.PhotoStorageObjectNotFoundException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class FakePhotoStorage implements PhotoStorage {

    private final Map<String, StoredPhoto> photos = new ConcurrentHashMap<>();
    private final AtomicBoolean failNextUpload = new AtomicBoolean();
    private final AtomicBoolean failNextOpen = new AtomicBoolean();
    private final AtomicBoolean failNextDelete = new AtomicBoolean();

    @Override
    public void upload(final String storageKey, final byte[] content, final String contentType) {
        if (failNextUpload.compareAndSet(true, false)) {
            throw new PhotoStorageException(new IllegalStateException("fake upload failure"));
        }
        photos.put(storageKey, new StoredPhoto(content.clone(), contentType));
    }

    @Override
    public InputStream open(final String storageKey) {
        if (failNextOpen.compareAndSet(true, false)) {
            throw new PhotoStorageException(new IllegalStateException("fake open failure"));
        }
        final StoredPhoto photo = photos.get(storageKey);
        if (photo == null) {
            throw new PhotoStorageObjectNotFoundException(new IllegalStateException("fake object not found"));
        }
        return new ByteArrayInputStream(photo.content());
    }

    @Override
    public void deleteIfExists(final String storageKey) {
        if (failNextDelete.compareAndSet(true, false)) {
            throw new PhotoStorageException(new IllegalStateException("fake delete failure"));
        }
        photos.remove(storageKey);
    }

    public void reset() {
        photos.clear();
        failNextUpload.set(false);
        failNextOpen.set(false);
        failNextDelete.set(false);
    }

    public void failNextUpload() {
        failNextUpload.set(true);
    }

    public void failNextOpen() {
        failNextOpen.set(true);
    }

    public void failNextDelete() {
        failNextDelete.set(true);
    }

    public boolean contains(final String storageKey) {
        return photos.containsKey(storageKey);
    }

    public int size() {
        return photos.size();
    }

    public Set<String> storageKeys() {
        return Set.copyOf(photos.keySet());
    }

    public byte[] content(final String storageKey) {
        return photos.get(storageKey).content().clone();
    }

    private record StoredPhoto(byte[] content, String contentType) {

        private StoredPhoto {
            content = content.clone();
        }

        @Override
        public byte[] content() {
            return content.clone();
        }
    }
}
