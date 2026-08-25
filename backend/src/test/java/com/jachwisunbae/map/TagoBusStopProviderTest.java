package com.jachwisunbae.map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TagoBusStopProviderTest {

    private MockRestServiceServer server;
    private TagoBusStopProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://apis.data.go.kr");
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new TagoBusStopProvider(builder.build(), "decoded-test-key");
    }

    @Test
    void nearbyReturnsActualStopsInsideTagoRadius() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString(
                        "/1613000/BusSttnInfoInqireService/getCrdntPrxmtSttnList")))
                .andExpect(queryParam("serviceKey", "decoded-test-key"))
                .andExpect(queryParam("pageNo", "1"))
                .andExpect(queryParam("numOfRows", "100"))
                .andExpect(queryParam("_type", "json"))
                .andExpect(queryParam("gpsLati", "37.5665"))
                .andExpect(queryParam("gpsLong", "126.978"))
                .andRespond(withSuccess(successResponse(), MediaType.APPLICATION_JSON));

        List<NearbyPlace> places = provider.nearby(
                BigDecimal.valueOf(37.5665), BigDecimal.valueOf(126.978), 2000);

        assertThat(places).hasSize(1);
        assertThat(places.getFirst()).satisfies(place -> {
            assertThat(place.providerPlaceId()).isEqualTo("tago:11:SEOUL001");
            assertThat(place.name()).isEqualTo("시청앞덕수궁");
            assertThat(place.category()).isEqualTo(MapCategory.TRANSPORT);
            assertThat(place.address()).isEqualTo("버스정류소");
            assertThat(place.distanceMeters()).isLessThan(100);
        });
        server.verify();
    }

    @Test
    void nearbySupportsSingleItemResponses() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("getCrdntPrxmtSttnList")))
                .andRespond(withSuccess(singleItemResponse(), MediaType.APPLICATION_JSON));

        List<NearbyPlace> places = provider.nearby(
                BigDecimal.valueOf(37.5665), BigDecimal.valueOf(126.978), 500);

        assertThat(places).extracting(NearbyPlace::name).containsExactly("시청앞덕수궁");
        server.verify();
    }

    private String successResponse() {
        return """
                {
                  "response": {
                    "header": {"resultCode": "00", "resultMsg": "NORMAL SERVICE."},
                    "body": {
                      "items": {"item": [
                        {"gpslati": "37.5670", "gpslong": "126.9780", "nodeid": "SEOUL001",
                         "nodenm": "시청앞덕수궁", "citycode": "11"},
                        {"gpslati": "37.5800", "gpslong": "126.9780", "nodeid": "SEOUL999",
                         "nodenm": "먼정류소", "citycode": "11"}
                      ]},
                      "totalCount": 2
                    }
                  }
                }
                """;
    }

    private String singleItemResponse() {
        return """
                {
                  "response": {
                    "header": {"resultCode": "00", "resultMsg": "NORMAL SERVICE."},
                    "body": {"items": {"item":
                      {"gpslati": "37.5670", "gpslong": "126.9780", "nodeid": "SEOUL001",
                       "nodenm": "시청앞덕수궁", "citycode": "11"}
                    }, "totalCount": 1}
                  }
                }
                """;
    }
}
