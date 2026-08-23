package com.jachwisunbae.map;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record NearbyResponse(Center center, int radius, Map<MapCategory, Integer> counts,
                             List<NearbyPlace> places) {
    public record Center(BigDecimal latitude, BigDecimal longitude) {
    }
}
