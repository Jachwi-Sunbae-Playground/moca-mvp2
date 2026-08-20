package com.jachwisunbae.auth.web;

import com.jachwisunbae.common.exception.DomainErrorCode;
import com.jachwisunbae.common.web.error.DomainErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class AuthenticationErrorWriter {

    private static final String MESSAGE = "인증 정보가 올바르지 않습니다.";
    private final ObjectMapper objectMapper;

    public AuthenticationErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getWriter(),
                new DomainErrorResponse(DomainErrorCode.ACCESS_TOKEN_INVALID.name(), MESSAGE));
    }
}
