package com.jachwisunbae.map;

import com.fasterxml.jackson.databind.JsonNode;
import com.jachwisunbae.common.exception.BusinessException;
import com.jachwisunbae.common.exception.DomainErrorCode;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "map.provider.mode", havingValue = "kakao")
public class KakaoMapProvider implements MapProvider {

    private static final int PAGE_SIZE = 15;
    private static final int MAX_PAGE_COUNT = 3;
    private static final Logger LOG = LoggerFactory.getLogger(KakaoMapProvider.class);
    private final RestClient client;
    private final Optional<BusStopProvider> busStopProvider;

    @Autowired
    public KakaoMapProvider(@Value("${map.kakao.rest-api-key}") String restApiKey,
                            @Value("${map.connect-timeout-millis:2000}") long connectTimeoutMillis,
                            @Value("${map.read-timeout-millis:5000}") long readTimeoutMillis,
                            Optional<BusStopProvider> busStopProvider) {
        this(createClient(restApiKey, connectTimeoutMillis, readTimeoutMillis), busStopProvider);
    }

    private static RestClient createClient(String restApiKey, long connectTimeoutMillis, long readTimeoutMillis) {
        if (restApiKey == null || restApiKey.isBlank()) {
            throw new IllegalStateException("kakao 지도 모드에는 KAKAO_REST_API_KEY가 필요합니다.");
        }
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMillis))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMillis));
        return RestClient.builder()
                .baseUrl("https://dapi.kakao.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + restApiKey)
                .requestFactory(requestFactory)
                .build();
    }

    KakaoMapProvider(RestClient client) {
        this(client, Optional.empty());
    }

    KakaoMapProvider(RestClient client, BusStopProvider busStopProvider) {
        this(client, Optional.of(busStopProvider));
    }

    private KakaoMapProvider(RestClient client, Optional<BusStopProvider> busStopProvider) {
        this.client = client;
        this.busStopProvider = busStopProvider;
    }

    @Override
    public List<MapAddress> geocode(String query) {
        JsonNode root = request(uri -> uri.path("/v2/local/search/address.json").queryParam("query", query).build());
        List<MapAddress> results = new ArrayList<>();
        for (JsonNode document : root.path("documents")) {
            JsonNode road = document.path("road_address");
            JsonNode jibun = document.path("address");
            results.add(new MapAddress(textOrNull(road, "address_name"), textOrNull(jibun, "address_name"),
                    decimal(document, "y"), decimal(document, "x")));
            if (results.size() == 10) {
                break;
            }
        }
        return results;
    }

    @Override
    public MapAddress reverseGeocode(BigDecimal latitude, BigDecimal longitude) {
        JsonNode root = request(uri -> uri.path("/v2/local/geo/coord2address.json")
                .queryParam("x", longitude).queryParam("y", latitude).build());
        JsonNode document = root.path("documents").path(0);
        if (document.isMissingNode()) {
            return new MapAddress(null, null, latitude, longitude);
        }
        return new MapAddress(textOrNull(document.path("road_address"), "address_name"),
                textOrNull(document.path("address"), "address_name"), latitude, longitude);
    }

    @Override
    public List<NearbyPlace> nearby(BigDecimal latitude, BigDecimal longitude, int radius,
                                    Set<MapCategory> categories) {
        Map<String, NearbyPlace> unique = new LinkedHashMap<>();
        for (MapCategory category : categories) {
            String code = categoryCode(category);
            appendPages(unique, category, page -> request(uri -> uri.path("/v2/local/search/category.json")
                    .queryParam("category_group_code", code)
                    .queryParam("x", longitude).queryParam("y", latitude)
                    .queryParam("radius", radius).queryParam("sort", "distance")
                    .queryParam("page", page).queryParam("size", PAGE_SIZE).build()));
            if (category == MapCategory.TRANSPORT) {
                appendBusStops(unique, latitude, longitude, radius);
            }
        }
        return List.copyOf(unique.values());
    }

    private void appendBusStops(Map<String, NearbyPlace> unique, BigDecimal latitude, BigDecimal longitude,
                                int radius) {
        busStopProvider.ifPresent(provider -> {
            try {
                provider.nearby(latitude, longitude, radius)
                        .forEach(place -> unique.putIfAbsent(place.providerPlaceId(), place));
            } catch (RuntimeException exception) {
                LOG.warn("TAGO 버스정류소 조회에 실패해 Kakao 지하철역 결과만 반환합니다.", exception);
            }
        });
    }

    private void appendPages(Map<String, NearbyPlace> unique, MapCategory category, IntFunction<JsonNode> fetchPage) {
        for (int page = 1; page <= MAX_PAGE_COUNT; page++) {
            JsonNode root = fetchPage.apply(page);
            appendPlaces(unique, root, category);
            if (root.path("meta").path("is_end").asBoolean(true)) {
                return;
            }
        }
    }

    private void appendPlaces(Map<String, NearbyPlace> unique, JsonNode root, MapCategory category) {
        for (JsonNode document : root.path("documents")) {
            String id = document.path("id").asText();
            unique.putIfAbsent(id, new NearbyPlace(id, document.path("place_name").asText(), category,
                    document.path("road_address_name").asText(document.path("address_name").asText()),
                    decimal(document, "y"), decimal(document, "x"), document.path("distance").asInt()));
        }
    }

    private String categoryCode(MapCategory category) {
        return switch (category) {
            case HOSPITAL -> "HP8";
            case TRANSPORT -> "SW8";
            case SCHOOL -> "SC4";
            case CONVENIENCE -> "CS2";
            case AGENCY -> "AG2";
        };
    }

    private JsonNode request(java.util.function.Function<org.springframework.web.util.UriBuilder, java.net.URI> uri) {
        try {
            JsonNode result = client.get().uri(uri).retrieve().body(JsonNode.class);
            return result == null ? com.fasterxml.jackson.databind.node.MissingNode.getInstance() : result;
        } catch (RuntimeException exception) {
            throw new BusinessException(DomainErrorCode.MAP_PROVIDER_UNAVAILABLE,
                    "지도 공급자 요청에 실패했습니다.", exception);
        }
    }

    private BigDecimal decimal(JsonNode node, String name) {
        return new BigDecimal(node.path(name).asText("0"));
    }

    private String textOrNull(JsonNode node, String name) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.path(name).asText();
        return value.isBlank() ? null : value;
    }
}
