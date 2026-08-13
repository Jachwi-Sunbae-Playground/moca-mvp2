package com.jachwisunbae.property.domain;

import java.net.URI;
import java.net.URISyntaxException;

public record DiscoverySource(DiscoverySourceType type, String value) {

    private static final int MAX_LENGTH = 500;

    public DiscoverySource {
        if (type == null || value == null) {
            throw new IllegalArgumentException("발견 경로는 필수입니다.");
        }
        value = value.trim();
        final int length = value.codePointCount(0, value.length());
        if (length < 1 || length > MAX_LENGTH) {
            throw new IllegalArgumentException("발견 경로 길이가 올바르지 않습니다.");
        }
    }

    public static DiscoverySource from(final String value) {
        if (value == null) {
            throw new IllegalArgumentException("발견 경로는 필수입니다.");
        }
        final String trimmedValue = value.trim();
        return new DiscoverySource(classify(trimmedValue), trimmedValue);
    }

    private static DiscoverySourceType classify(final String value) {
        try {
            final URI uri = new URI(value);
            final String scheme = uri.getScheme();
            if (scheme != null
                    && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                    && uri.getHost() != null) {
                return DiscoverySourceType.URL;
            }
        } catch (URISyntaxException ignored) {
            // URI가 아니어도 일반 텍스트 발견 경로로 보존한다.
        }
        return DiscoverySourceType.TEXT;
    }
}
