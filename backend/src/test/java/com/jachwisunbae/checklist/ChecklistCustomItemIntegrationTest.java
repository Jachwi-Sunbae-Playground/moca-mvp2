package com.jachwisunbae.checklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jachwisunbae.common.IntegrationTest;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class ChecklistCustomItemIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 직접_질문을_저장하고_매물_교체와_PDF까지_스냅샷을_유지한다() throws Exception {
        String token = nicknameLoginToken("직접질문테스터");
        MvcResult created = mockMvc.perform(post("/api/checklists")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"나만의 온라인 확인",
                                  "stage":"ONLINE_PHONE",
                                  "items":[
                                    {"systemCheckItemId":101},
                                    {"systemCheckItemId":null,"question":"  창틀 곰팡이는 괜찮은가?  "}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.items[0].origin").value("PROVIDED"))
                .andExpect(jsonPath("$.data.items[1].origin").value("CUSTOM"))
                .andExpect(jsonPath("$.data.items[1].systemCheckItemId").doesNotExist())
                .andExpect(jsonPath("$.data.items[1].question").value("창틀 곰팡이는 괜찮은가?"))
                .andReturn();
        long checklistId = data(created).path("id").asLong();

        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM user_checklist_items
                WHERE user_checklist_id = ? AND system_check_item_id IS NULL
                """, Long.class, checklistId)).isEqualTo(1L);

        mockMvc.perform(put("/api/checklists/{checklistId}", checklistId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"나만의 온라인 확인",
                                  "items":[
                                    {"systemCheckItemId":null,"question":"창틀 곰팡이는 괜찮은가?"},
                                    {"systemCheckItemId":101}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].origin").value("CUSTOM"))
                .andExpect(jsonPath("$.data.items[0].displayOrder").value(1));

        long propertyId = createProperty(token, "직접 질문 매물");
        long secondPropertyId = createProperty(token, "비교 매물");
        MvcResult applied = mockMvc.perform(put(
                        "/api/properties/{propertyId}/checklists/ONLINE_PHONE", propertyId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceType\":\"USER\",\"checklistId\":" + checklistId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].systemCheckItemId").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].question").value("창틀 곰팡이는 괜찮은가?"))
                .andReturn();
        long propertyChecklistId = data(applied).path("id").asLong();
        long customItemId = data(applied).path("items").path(0).path("id").asLong();

        mockMvc.perform(patch(
                        "/api/properties/{propertyId}/checklists/{checklistId}/items/{itemId}/status",
                        propertyId, propertyChecklistId, customItemId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"GOOD\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch(
                        "/api/properties/{propertyId}/checklists/{checklistId}/items/{itemId}/memo",
                        propertyId, propertyChecklistId, customItemId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memo\":\"직접 확인 완료\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/properties/{propertyId}/checklists/ONLINE_PHONE", propertyId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceType\":\"USER\",\"checklistId\":" + checklistId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].status").value("GOOD"))
                .andExpect(jsonPath("$.data.items[0].memo").value("직접 확인 완료"));

        mockMvc.perform(post("/api/properties/export.pdf")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"propertyIds\":[" + propertyId + "," + secondPropertyId + "]}"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    try (var document = Loader.loadPDF(result.getResponse().getContentAsByteArray())) {
                        String text = new PDFTextStripper().getText(document);
                        assertThat(text).contains("창틀 곰팡이는 괜찮은가?", "직접 확인 완료");
                    }
                });
    }

    @Test
    void 시스템_ID와_직접_질문을_동시에_보낸_항목은_거절한다() throws Exception {
        String token = nicknameLoginToken("잘못된질문테스터");
        mockMvc.perform(post("/api/checklists")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"잘못된 목록",
                                  "stage":"ONLINE_PHONE",
                                  "items":[{"systemCheckItemId":101,"question":"동시에 보낸 질문"}]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHECKLIST_ITEMS_INVALID"));
    }

    private long createProperty(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/properties")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"%s",
                                  "roadAddress":"서울 관악구 신림로 12길 3",
                                  "jibunAddress":"서울 관악구 신림동 123-4",
                                  "latitude":37.4841234,
                                  "longitude":126.9291234,
                                  "depositAmount":10000000,
                                  "monthlyRentAmount":550000,
                                  "discoverySource":"중개사 010-1234-5678"
                                }
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();
        return data(result).path("id").asLong();
    }

    private String nicknameLoginToken(String nickname) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"" + nickname + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return data(result).path("accessToken").asText();
    }

    private JsonNode data(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray()).path("data");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
