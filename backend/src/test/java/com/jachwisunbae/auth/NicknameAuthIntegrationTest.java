package com.jachwisunbae.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jachwisunbae.common.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class NicknameAuthIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 비밀번호_없는_닉네임은_정규화된_같은_이름으로_같은_회원에_접근한다() throws Exception {
        JsonNode created = login("  공유닉네임  ", null, 200);
        JsonNode returned = login("공유닉네임", null, 200);

        assertThat(returned.path("member").path("memberId").asLong())
                .isEqualTo(created.path("member").path("memberId").asLong());
        assertThat(returned.path("member").path("passwordProtected").asBoolean()).isFalse();
    }

    @Test
    void 보호된_닉네임은_같은_비밀번호만_허용하고_해시는_원문을_저장하지_않는다() throws Exception {
        JsonNode created = login("보호닉네임", "safe-password", 200);

        login("보호닉네임", null, 401);
        login("보호닉네임", "wrong-password", 401);
        JsonNode returned = login("보호닉네임", "safe-password", 200);

        assertThat(returned.path("member").path("memberId").asLong())
                .isEqualTo(created.path("member").path("memberId").asLong());
        assertThat(returned.path("member").path("passwordProtected").asBoolean()).isTrue();
        String passwordHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM nickname_credentials WHERE nickname_key = ?",
                String.class, "보호닉네임");
        assertThat(passwordHash).startsWith("$2").doesNotContain("safe-password");
    }

    @Test
    void 비밀번호_없는_기존_닉네임에_비밀번호를_붙여_가로챌_수_없다() throws Exception {
        login("열린닉네임", null, 200);

        mockMvc.perform(post("/api/auth/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"열린닉네임\",\"password\":\"claim-password\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NICKNAME_PASSWORD_UNEXPECTED"));
    }

    @Test
    void 선택_비밀번호는_사용하면_최소_네_글자여야_한다() throws Exception {
        mockMvc.perform(post("/api/auth/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"짧은비밀번호\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NICKNAME_PASSWORD_INVALID"));
    }

    @Test
    void 보호된_닉네임의_연속_실패는_일시적으로_제한한다() throws Exception {
        login("요청제한닉네임", "correct-password", 200);

        for (int index = 0; index < 5; index++) {
            login("요청제한닉네임", "wrong-password", 401);
        }
        mockMvc.perform(post("/api/auth/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"요청제한닉네임\",\"password\":\"correct-password\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("NICKNAME_AUTH_RATE_LIMITED"));
    }

    @Test
    void 발급된_토큰으로_닉네임과_보호_상태를_조회한다() throws Exception {
        JsonNode login = login("내정보닉네임", "my-password", 200);

        mockMvc.perform(get("/api/members/me")
                        .header("Authorization", "Bearer " + login.path("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("내정보닉네임"))
                .andExpect(jsonPath("$.data.passwordProtected").value(true))
                .andExpect(jsonPath("$.data.email").doesNotExist());
    }

    private JsonNode login(String nickname, String password, int expectedStatus) throws Exception {
        String passwordField = password == null ? "" : ",\"password\":\"" + password + "\"";
        MvcResult result = mockMvc.perform(post("/api/auth/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"" + nickname + "\"" + passwordField + "}"))
                .andExpect(status().is(expectedStatus))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsByteArray()).path("data");
    }
}
