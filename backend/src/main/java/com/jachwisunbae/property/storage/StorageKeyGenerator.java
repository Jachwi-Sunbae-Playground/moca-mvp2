package com.jachwisunbae.property.storage;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class StorageKeyGenerator {

    public String generate(final long memberId, final long propertyId) {
        return "members/%d/properties/%d/%s".formatted(memberId, propertyId, UUID.randomUUID());
    }
}
