package com.jachwisunbae.map;

import com.jachwisunbae.common.exception.BusinessException;
import com.jachwisunbae.common.exception.DomainErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MapService {

    private static final Set<Integer> SUPPORTED_RADII = Set.of(500, 1000, 2000);
    private final MapProvider provider;
    private final Clock clock;
    private final long cacheTtlSeconds;
    private final Map<CacheKey, CacheEntry> nearbyCache = new ConcurrentHashMap<>();

    public MapService(MapProvider provider, Clock clock,
                      @Value("${map.cache-ttl-seconds:600}") long cacheTtlSeconds) {
        this.provider = provider;
        this.clock = clock;
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    public List<MapAddress> geocode(String query) {
        if (query == null || query.isBlank() || query.length() > 200) {
            throw invalidQuery("주소 검색어는 1자 이상 200자 이하여야 합니다.");
        }
        return provider.geocode(query.trim());
    }

    public MapAddress reverseGeocode(BigDecimal latitude, BigDecimal longitude) {
        validateCoordinates(latitude, longitude);
        return provider.reverseGeocode(latitude, longitude);
    }

    public NearbyResponse nearby(BigDecimal latitude, BigDecimal longitude, int radius,
                                 Set<MapCategory> requestedCategories) {
        validateCoordinates(latitude, longitude);
        if (!SUPPORTED_RADII.contains(radius)) {
            throw invalidQuery("반경은 500m, 1km, 2km만 사용할 수 있습니다.");
        }
        Set<MapCategory> categories = requestedCategories == null || requestedCategories.isEmpty()
                ? EnumSet.allOf(MapCategory.class)
                : EnumSet.copyOf(requestedCategories);
        CacheKey key = new CacheKey(latitude.setScale(4, RoundingMode.HALF_UP),
                longitude.setScale(4, RoundingMode.HALF_UP), radius, EnumSet.copyOf(categories));
        CacheEntry cached = nearbyCache.get(key);
        Instant now = clock.instant();
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.response();
        }
        List<NearbyPlace> places = provider.nearby(latitude, longitude, radius, categories);
        Map<MapCategory, Integer> counts = new EnumMap<>(MapCategory.class);
        for (MapCategory category : MapCategory.values()) {
            counts.put(category, 0);
        }
        places.forEach(place -> counts.computeIfPresent(place.category(), (category, count) -> count + 1));
        NearbyResponse response = new NearbyResponse(new NearbyResponse.Center(latitude, longitude), radius,
                Map.copyOf(counts), places);
        nearbyCache.put(key, new CacheEntry(response, now.plusSeconds(cacheTtlSeconds)));
        return response;
    }

    private void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null
                || latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitude.compareTo(BigDecimal.valueOf(90)) > 0
                || longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw invalidQuery("위도와 경도 범위가 올바르지 않습니다.");
        }
    }

    private BusinessException invalidQuery(String message) {
        return new BusinessException(DomainErrorCode.MAP_QUERY_INVALID, message);
    }

    private record CacheKey(BigDecimal latitude, BigDecimal longitude, int radius,
                            Set<MapCategory> categories) {
    }

    private record CacheEntry(NearbyResponse response, Instant expiresAt) {
    }
}
