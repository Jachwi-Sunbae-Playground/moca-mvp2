package com.jachwisunbae.map;

import java.math.BigDecimal;

public record NearbyPlace(String providerPlaceId, String name, MapCategory category,
                          String address, BigDecimal latitude, BigDecimal longitude,
                          int distanceMeters) {
}
