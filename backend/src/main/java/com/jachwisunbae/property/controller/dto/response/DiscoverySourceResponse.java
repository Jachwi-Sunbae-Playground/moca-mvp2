package com.jachwisunbae.property.controller.dto.response;

import com.jachwisunbae.property.domain.DiscoverySource;

public record DiscoverySourceResponse(String type, String value) {

    public static DiscoverySourceResponse from(final DiscoverySource source) {
        return new DiscoverySourceResponse(source.type().name(), source.value());
    }
}
