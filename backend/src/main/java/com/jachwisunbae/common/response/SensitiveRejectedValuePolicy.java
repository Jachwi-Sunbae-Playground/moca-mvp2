package com.jachwisunbae.common.response;

import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SensitiveRejectedValuePolicy {

    private static final Set<String> SENSITIVE_FIELD_FRAGMENTS = Set.of(
            "authorizationcode",
            "codeverifier",
            "nonce",
            "token",
            "secret",
            "password",
            "memo",
            "content",
            "discoverysource",
            "file",
            "photo"
    );

    public Object sanitize(final String field, final Object rejectedValue) {
        final String normalizedField = field.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        final boolean sensitive = SENSITIVE_FIELD_FRAGMENTS.stream().anyMatch(normalizedField::contains);

        if (sensitive) {
            return null;
        }
        return rejectedValue;
    }
}
