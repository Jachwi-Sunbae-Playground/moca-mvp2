package com.jachwisunbae.common.web.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jachwisunbae.common.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class GlobalExceptionHandlerTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 잘못된_열거형은_필드_오류로_응답한다() throws Exception {
        mockMvc.perform(get("/api/check-items").param("stage", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.errors[0].field").value("stage"))
                .andExpect(jsonPath("$.errors[0].reason").value("올바른 형식의 값이 아닙니다."));
    }

    @Test
    void 읽을_수_없는_JSON은_공통_오류로_응답한다() throws Exception {
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void 존재하지_않는_경로는_공통_오류로_응답한다() throws Exception {
        mockMvc.perform(get("/not-existing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
