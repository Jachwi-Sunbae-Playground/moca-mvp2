package com.jachwisunbae.map;

import com.fasterxml.jackson.databind.JsonNode;
import com.jachwisunbae.common.exception.BusinessException;
import com.jachwisunbae.common.exception.DomainErrorCode;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "map.bus-stops.provider", havingValue = "tago")
public class TagoBusStopProvider implements BusStopProvider {

    private static final int TAGO_RADIUS_METERS = 500;
    private static final int PAGE_SIZE = 100;
    private static final double EARTH_RADIUS_METERS = 6_371_000;
    private final RestClient client;
    private final String serviceKey;

    @Autowired
    public TagoBusStopProvider(@Value("${map.bus-stops.tago.service-key}") String serviceKey,
                               @Value("${map.connect-timeout-millis:2000}") long connectTimeoutMillis,
                               @Value("${map.read-timeout-millis:5000}") long readTimeoutMillis) {
        this(createClient(connectTimeoutMillis, readTimeoutMillis), serviceKey);
    }

    TagoBusStopProvider(RestClient client, String serviceKey) {
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new IllegalStateException("tago 버스정류소 모드에는 DATA_GO_KR_SERVICE_KEY가 필요합니다.");
        }
        this.client = client;
        this.serviceKey = serviceKey;
    }

    private static RestClient createClient(long connectTimeoutMillis, long readTimeoutMillis) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMillis))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMillis));
        return RestClient.builder()
                .baseUrl("https://apis.data.go.kr")
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public List<NearbyPlace> nearby(BigDecimal latitude, BigDecimal longitude, int radius) {
        int effectiveRadius = Math.min(radius, TAGO_RADIUS_METERS);
        JsonNode root = request(latitude, longitude);
        JsonNode response = root.path("response");
        String resultCode = response.path("header").path("resultCode").asText();
        if (!("00".equals(resultCode) || "0000".equals(resultCode))) {
            throw unavailable("TAGO 버스정류소 응답이 정상이 아닙니다.");
        }

        JsonNode item = response.path("body").path("items").path("item");
        List<NearbyPlace> places = new ArrayList<>();
        if (item.isArray()) {
            item.forEach(node -> append(places, node, latitude, longitude, effectiveRadius));
        } else if (item.isObject()) {
            append(places, item, latitude, longitude, effectiveRadius);
        }
        return List.copyOf(places);
    }

    private JsonNode request(BigDecimal latitude, BigDecimal longitude) {
        try {
            JsonNode result = client.get().uri(uri -> uri
                    .path("/1613000/BusSttnInfoInqireService/getCrdntPrxmtSttnList")
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", PAGE_SIZE)
                    .queryParam("_type", "json")
                    .queryParam("gpsLati", latitude)
                    .queryParam("gpsLong", longitude)
                    .build()).retrieve().body(JsonNode.class);
            return result == null ? com.fasterxml.jackson.databind.node.MissingNode.getInstance() : result;
        } catch (RuntimeException exception) {
            throw unavailable("TAGO 버스정류소 요청에 실패했습니다.", exception);
        }
    }

    private void append(List<NearbyPlace> places, JsonNode node, BigDecimal latitude, BigDecimal longitude,
                        int effectiveRadius) {
        BigDecimal stopLatitude = decimal(node, "gpslati");
        BigDecimal stopLongitude = decimal(node, "gpslong");
        int distance = distanceMeters(latitude, longitude, stopLatitude, stopLongitude);
        if (distance > effectiveRadius) {
            return;
        }
        String nodeId = node.path("nodeid").asText();
        String cityCode = node.path("citycode").asText();
        String providerId = "tago:" + cityCode + ":" + nodeId;
        places.add(new NearbyPlace(providerId, node.path("nodenm").asText("버스정류소"),
                MapCategory.TRANSPORT, "버스정류소", stopLatitude, stopLongitude, distance));
    }

    private int distanceMeters(BigDecimal fromLatitude, BigDecimal fromLongitude,
                               BigDecimal toLatitude, BigDecimal toLongitude) {
        double fromLat = Math.toRadians(fromLatitude.doubleValue());
        double toLat = Math.toRadians(toLatitude.doubleValue());
        double deltaLat = toLat - fromLat;
        double deltaLon = Math.toRadians(toLongitude.doubleValue() - fromLongitude.doubleValue());
        double haversine = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(fromLat) * Math.cos(toLat) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double distance = 2 * EARTH_RADIUS_METERS * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
        return (int) Math.round(distance);
    }

    private BigDecimal decimal(JsonNode node, String name) {
        String value = node.path(name).asText();
        if (value.isBlank()) {
            throw unavailable("TAGO 버스정류소 좌표가 비어 있습니다.");
        }
        return new BigDecimal(value);
    }

    private BusinessException unavailable(String message) {
        return new BusinessException(DomainErrorCode.MAP_PROVIDER_UNAVAILABLE, message);
    }

    private BusinessException unavailable(String message, Throwable cause) {
        return new BusinessException(DomainErrorCode.MAP_PROVIDER_UNAVAILABLE, message, cause);
    }
}
