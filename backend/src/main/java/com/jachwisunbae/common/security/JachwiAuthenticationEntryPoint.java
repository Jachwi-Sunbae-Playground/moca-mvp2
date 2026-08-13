package com.jachwisunbae.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.response.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class JachwiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final AccessTokenErrorClassifier errorClassifier;

    public JachwiAuthenticationEntryPoint(
            final ObjectMapper objectMapper,
            final AccessTokenErrorClassifier errorClassifier
    ) {
        this.objectMapper = objectMapper;
        this.errorClassifier = errorClassifier;
    }

    @Override
    public void commence(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final AuthenticationException authException
    ) throws IOException, ServletException {
        final ErrorCode errorCode = errorClassifier.classify(authException);
        response.setStatus(errorCode.getStatus().value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ErrorResponse.from(errorCode));
    }
}
