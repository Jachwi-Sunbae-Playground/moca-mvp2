package com.jachwisunbae.mvp2;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasKey;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jachwisunbae.common.IntegrationTest;
import com.jachwisunbae.property.storage.PhotoStorage;
import com.jachwisunbae.auth.token.JwtTokenProvider;
import java.util.Base64;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.jdbc.core.JdbcTemplate;

class Mvp2LocalFlowIntegrationTest extends IntegrationTest {

    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M/wHwAF/gL+Xw2YAAAAAElFTkSuQmCC");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private PhotoStorage photoStorage;

    @Test
    void 닉네임_로그인부터_매물_메모_체크_사진_지도_삭제까지_동작한다() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths", hasKey("/api/maps/nearby")))
                .andExpect(jsonPath("$.paths", hasKey("/api/properties/export.csv")))
                .andExpect(jsonPath("$.paths", hasKey("/api/properties/export.pdf")))
                .andExpect(jsonPath("$.paths", hasKey("/api/auth/nickname")))
                .andExpect(jsonPath("$.paths", hasKey("/api/properties/{propertyId}/photos/{photoId}")));

        String token = nicknameLoginToken("통합테스터", null);

        mockMvc.perform(get("/api/maps/geocode")
                        .header("Authorization", bearer(token))
                        .param("query", "신림역"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].roadAddress").value("서울 관악구 신림로 12길 3"));

        long propertyId = createProperty(token);

        String otherMemberToken = nicknameLoginToken("다른테스터", "other-safe-password");
        mockMvc.perform(get("/api/properties/{propertyId}", propertyId)
                        .header("Authorization", bearer(otherMemberToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/properties/{propertyId}", propertyId)
                        .header("Authorization", bearer(jwtTokenProvider.createAccessToken(999_999L))))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/properties/{propertyId}", propertyId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.address").value("서울 관악구 신림로 12길 3"))
                .andExpect(jsonPath("$.data.latitude").value(37.4841234));

        mockMvc.perform(get("/api/properties/export.csv")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray())
                        .startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF));

        mockMvc.perform(post("/api/properties/{propertyId}/memo", propertyId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(4)));

        mockMvc.perform(put("/api/properties/{propertyId}/memo", propertyId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[
                                  {"systemMemoItemId":1,"content":"9월 1일"},
                                  {"systemMemoItemId":2,"content":"에어컨"},
                                  {"systemMemoItemId":3,"content":"수도 포함"},
                                  {"systemMemoItemId":4,"content":"토요일 14시"}
                                ],"freeMemo":"창문 방향 재확인"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].content").value("9월 1일"))
                .andExpect(jsonPath("$.data.freeMemo").value("창문 방향 재확인"));

        MvcResult applied = mockMvc.perform(put("/api/properties/{propertyId}/checklists/ONLINE_PHONE", propertyId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceType\":\"SYSTEM_DEFAULT\",\"checklistId\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourceChecklistId").doesNotExist())
                .andExpect(jsonPath("$.data.items", hasSize(6)))
                .andReturn();
        JsonNode appliedData = data(applied);
        long propertyChecklistId = appliedData.path("id").asLong();
        long itemId = appliedData.path("items").path(0).path("id").asLong();

        mockMvc.perform(patch("/api/properties/{propertyId}/checklists/{checklistId}/items/{itemId}/status",
                        propertyId, propertyChecklistId, itemId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"GOOD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.item.status").value("GOOD"));

        mockMvc.perform(patch("/api/properties/{propertyId}/checklists/{checklistId}/items/{itemId}/memo",
                        propertyId, propertyChecklistId, itemId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memo\":\"전화로 확인함\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.item.memo").value("전화로 확인함"));

        mockMvc.perform(get("/api/properties/{propertyId}/checklists", propertyId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.overallProgress.totalCount").value(6))
                .andExpect(jsonPath("$.data.overallProgress.completedCount").value(1))
                .andExpect(jsonPath("$.data.stages[0].progress.completedCount").value(1))
                .andExpect(jsonPath("$.data.stages[1].applied").value(false));

        mockMvc.perform(get("/api/properties")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].stages", hasSize(3)))
                .andExpect(jsonPath("$.data.items[0].stages[0].progress.completedCount").value(1));

        when(photoStorage.download(anyString())).thenReturn(PNG);
        MockMultipartFile file = new MockMultipartFile("file", "room.png", "image/png", PNG);
        MvcResult uploaded = mockMvc.perform(multipart("/api/properties/{propertyId}/photos", propertyId)
                        .file(file)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.representative").value(true))
                .andReturn();
        long photoId = data(uploaded).path("id").asLong();

        mockMvc.perform(get("/api/properties/{propertyId}/photos/{photoId}", propertyId, photoId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(PNG));

        long secondPropertyId = createProperty(token);
        mockMvc.perform(post("/api/properties/export.pdf")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"propertyIds\":[" + propertyId + "," + secondPropertyId + "]}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(result -> {
                    byte[] pdf = result.getResponse().getContentAsByteArray();
                    assertThat(pdf).startsWith((byte) '%', (byte) 'P', (byte) 'D', (byte) 'F');
                    try (var document = Loader.loadPDF(pdf)) {
                        String text = new PDFTextStripper().getText(document);
                        assertThat(text)
                                .contains("매물 비교 기록")
                                .contains("통합 테스트 원룸")
                                .contains("창문 방향 재확인")
                                .contains("전화로 확인함");
                    }
                });

        mockMvc.perform(post("/api/properties/export.pdf")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"propertyIds\":[" + propertyId + "," + propertyId + "]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PROPERTY_INPUT_INVALID"));

        mockMvc.perform(get("/api/maps/nearby")
                        .header("Authorization", bearer(token))
                        .param("latitude", "37.4841234")
                        .param("longitude", "126.9291234")
                        .param("radius", "1000")
                        .param("categories", "HOSPITAL,TRANSPORT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.radius").value(1000))
                .andExpect(jsonPath("$.data.places", hasSize(4)));

        mockMvc.perform(delete("/api/properties/{propertyId}", propertyId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/properties/{propertyId}", propertyId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
        assertThat(countForProperty("property_photos", propertyId)).isZero();
        assertThat(countForProperty("property_memos", propertyId)).isZero();
        assertThat(countForProperty("property_checklists", propertyId)).isZero();
        verify(photoStorage, atLeastOnce()).delete(anyString());
    }

    private String nicknameLoginToken(String nickname, String password) throws Exception {
        String body = password == null
                ? "{\"nickname\":\"" + nickname + "\"}"
                : "{\"nickname\":\"" + nickname + "\",\"password\":\"" + password + "\"}";
        MvcResult result = mockMvc.perform(post("/api/auth/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.member.name").value(nickname))
                .andExpect(jsonPath("$.data.member.passwordProtected").value(password != null))
                .andReturn();
        return data(result).path("accessToken").asText();
    }

    private long createProperty(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/properties")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"통합 테스트 원룸","depositAmount":10000000,"monthlyRentAmount":550000,
                                 "discoverySource":"중개사 추천","roadAddress":"서울 관악구 신림로 12길 3",
                                 "jibunAddress":"서울 관악구 신림동 1433-12","latitude":37.4841234,"longitude":126.9291234}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.lastActivityAt").exists())
                .andReturn();
        return data(result).path("id").asLong();
    }

    private JsonNode data(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray()).path("data");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private long countForProperty(String table, long propertyId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE property_id = ?", Long.class, propertyId);
        return count == null ? 0L : count;
    }
}
