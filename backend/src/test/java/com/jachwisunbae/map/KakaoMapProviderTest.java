package com.jachwisunbae.map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KakaoMapProviderTest {

    private MockRestServiceServer server;
    private KakaoMapProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://dapi.kakao.com");
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new KakaoMapProvider(builder.build());
    }

    @Test
    void categorySearchReadsEveryExposedPage() {
        expectCategoryPage("HP8", 1, false, "hospital-1", "서울의원", "120");
        expectCategoryPage("HP8", 2, true, "hospital-2", "서울병원", "420");

        List<NearbyPlace> places = provider.nearby(
                BigDecimal.valueOf(37.5665), BigDecimal.valueOf(126.978), 1000, Set.of(MapCategory.HOSPITAL));

        assertThat(places).extracting(NearbyPlace::providerPlaceId).containsExactly("hospital-1", "hospital-2");
        server.verify();
    }

    @Test
    void transportSearchCombinesSubwayAndConfiguredBusStops() {
        BusStopProvider busStops = (latitude, longitude, radius) -> List.of(
                new NearbyPlace("tago:11:bus-1", "시청앞", MapCategory.TRANSPORT, "버스정류소",
                        BigDecimal.valueOf(37.565), BigDecimal.valueOf(126.978), 140),
                new NearbyPlace("tago:11:bus-2", "덕수궁", MapCategory.TRANSPORT, "버스정류소",
                        BigDecimal.valueOf(37.564), BigDecimal.valueOf(126.976), 260));
        RestClient.Builder builder = RestClient.builder().baseUrl("https://dapi.kakao.com");
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new KakaoMapProvider(builder.build(), busStops);
        expectCategoryPage("SW8", 1, true, "subway-1", "시청역", "80");

        List<NearbyPlace> places = provider.nearby(
                BigDecimal.valueOf(37.5665), BigDecimal.valueOf(126.978), 1000, Set.of(MapCategory.TRANSPORT));

        assertThat(places).extracting(NearbyPlace::providerPlaceId)
                .containsExactly("subway-1", "tago:11:bus-1", "tago:11:bus-2");
        assertThat(places).allMatch(place -> place.category() == MapCategory.TRANSPORT);
        server.verify();
    }

    @Test
    void transportSearchKeepsSubwayResultsWhenBusStopProviderFails() {
        BusStopProvider unavailableBusStops = (latitude, longitude, radius) -> {
            throw new IllegalStateException("TAGO unavailable");
        };
        RestClient.Builder builder = RestClient.builder().baseUrl("https://dapi.kakao.com");
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new KakaoMapProvider(builder.build(), unavailableBusStops);
        expectCategoryPage("SW8", 1, true, "subway-1", "시청역", "80");

        List<NearbyPlace> places = provider.nearby(
                BigDecimal.valueOf(37.5665), BigDecimal.valueOf(126.978), 1000, Set.of(MapCategory.TRANSPORT));

        assertThat(places).extracting(NearbyPlace::providerPlaceId).containsExactly("subway-1");
        server.verify();
    }

    private void expectCategoryPage(String category, int page, boolean end, String id, String name, String distance) {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/v2/local/search/category.json")))
                .andExpect(queryParam("category_group_code", category))
                .andExpect(queryParam("page", String.valueOf(page)))
                .andExpect(queryParam("size", "15"))
                .andRespond(withSuccess(response(end, id, name, distance), MediaType.APPLICATION_JSON));
    }

    private String response(boolean end, String id, String name, String distance) {
        return """
                {
                  "meta": {"is_end": %s},
                  "documents": [{
                    "id": "%s",
                    "place_name": "%s",
                    "road_address_name": "서울 중구 세종대로 110",
                    "address_name": "서울 중구 태평로1가 31",
                    "y": "37.5665",
                    "x": "126.978",
                    "distance": "%s"
                  }]
                }
                """.formatted(end, id, name, distance);
    }
}
