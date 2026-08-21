package com.jachwisunbae.common.config;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jachwisunbae.auth.token.JwtTokenProvider;
import com.jachwisunbae.auth.web.AuthenticationErrorWriter;
import com.jachwisunbae.auth.web.JwtAuthenticationFilter;
import com.jachwisunbae.common.exception.BusinessException;
import com.jachwisunbae.common.exception.DomainErrorCode;
import com.jachwisunbae.common.web.error.DomainErrorHttpMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = CorsConfigTest.ProtectedController.class,
        properties = "app.cors.allowed-origins=https://dev.jachwi-sunbae.kr")
@Import({CorsConfig.class, JwtAuthenticationFilter.class, AuthenticationErrorWriter.class})
class CorsConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private DomainErrorHttpMapper domainErrorHttpMapper;

    @Test
    void 허용된_Origin의_인증_실패_응답에_CORS_헤더를_추가한다() throws Exception {
        mockMvc.perform(get("/api/protected")
                        .header(HttpHeaders.ORIGIN, "https://dev.jachwi-sunbae.kr"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "https://dev.jachwi-sunbae.kr"))
                .andExpect(header().doesNotExist(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void 허용된_Origin의_잘못된_토큰_응답에_CORS_헤더를_추가한다() throws Exception {
        when(jwtTokenProvider.parseMemberId("invalid-token"))
                .thenThrow(new BusinessException(
                        DomainErrorCode.ACCESS_TOKEN_INVALID,
                        "Access Token이 올바르지 않습니다."));

        mockMvc.perform(get("/api/protected")
                        .header(HttpHeaders.ORIGIN, "https://dev.jachwi-sunbae.kr")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "https://dev.jachwi-sunbae.kr"));
    }

    @Test
    void 허용된_Origin의_사전_요청을_허용한다() throws Exception {
        mockMvc.perform(options("/api/protected")
                        .header(HttpHeaders.ORIGIN, "https://dev.jachwi-sunbae.kr")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "https://dev.jachwi-sunbae.kr"));
    }

    @Test
    void 허용되지_않은_Origin에는_CORS_헤더를_추가하지_않는다() throws Exception {
        mockMvc.perform(get("/api/protected")
                        .header(HttpHeaders.ORIGIN, "https://attacker.example"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @RestController
    static class ProtectedController {

        @GetMapping("/api/protected")
        String get() {
            return "ok";
        }
    }
}
