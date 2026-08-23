package com.jachwisunbae.map;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "map.provider.mode", havingValue = "demo", matchIfMissing = true)
public class DemoMapProvider implements MapProvider {

    private static final BigDecimal DEFAULT_LATITUDE = new BigDecimal("37.5665000");
    private static final BigDecimal DEFAULT_LONGITUDE = new BigDecimal("126.9780000");

    @Override
    public List<MapAddress> geocode(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String road = query.contains("신림")
                ? "서울 관악구 신림로 12길 3" : "서울 중구 세종대로 110";
        String jibun = query.contains("신림")
                ? "서울 관악구 신림동 1433-12" : "서울 중구 태평로1가 31";
        BigDecimal latitude = query.contains("신림") ? new BigDecimal("37.4841234") : DEFAULT_LATITUDE;
        BigDecimal longitude = query.contains("신림") ? new BigDecimal("126.9291234") : DEFAULT_LONGITUDE;
        return List.of(new MapAddress(road, jibun, latitude, longitude));
    }

    @Override
    public MapAddress reverseGeocode(BigDecimal latitude, BigDecimal longitude) {
        if (latitude.subtract(new BigDecimal("37.48")).abs().compareTo(new BigDecimal("0.08")) < 0) {
            return new MapAddress("서울 관악구 신림로 12길 3", "서울 관악구 신림동 1433-12",
                    latitude, longitude);
        }
        return new MapAddress("서울 중구 세종대로 110", "서울 중구 태평로1가 31", latitude, longitude);
    }

    @Override
    public List<NearbyPlace> nearby(BigDecimal latitude, BigDecimal longitude, int radius,
                                    Set<MapCategory> categories) {
        List<NearbyPlace> places = List.of(
                place("demo-hospital-1", "서울시립병원", MapCategory.HOSPITAL,
                        latitude, longitude, 320, "서울 중구 세종대로 92"),
                place("demo-hospital-2", "우리의원", MapCategory.HOSPITAL,
                        latitude, longitude, 740, "서울 중구 무교로 12"),
                place("demo-transport-1", "시청역", MapCategory.TRANSPORT,
                        latitude, longitude, 280, "서울 중구 세종대로 지하 101"),
                place("demo-transport-2", "시청앞 버스정류장", MapCategory.TRANSPORT,
                        latitude, longitude, 410, "서울 중구 태평로1가"),
                place("demo-school-1", "덕수초등학교", MapCategory.SCHOOL,
                        latitude, longitude, 830, "서울 중구 덕수궁길 140"),
                place("demo-convenience-1", "모카 편의점", MapCategory.CONVENIENCE,
                        latitude, longitude, 180, "서울 중구 세종대로 100"),
                place("demo-convenience-2", "24시 편의점", MapCategory.CONVENIENCE,
                        latitude, longitude, 620, "서울 중구 을지로 12"),
                place("demo-agency-1", "자취선배 공인중개사", MapCategory.AGENCY,
                        latitude, longitude, 510, "서울 중구 다동길 8")
        );
        return places.stream()
                .filter(place -> place.distanceMeters() <= radius && categories.contains(place.category()))
                .toList();
    }

    private NearbyPlace place(String id, String name, MapCategory category, BigDecimal latitude,
                              BigDecimal longitude, int distance, String address) {
        BigDecimal offset = BigDecimal.valueOf(distance).movePointLeft(6);
        return new NearbyPlace(id, name, category, address, latitude.add(offset), longitude.add(offset), distance);
    }
}
