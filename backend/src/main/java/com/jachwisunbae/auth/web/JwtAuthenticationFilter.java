package com.jachwisunbae.auth.web;

import com.jachwisunbae.auth.token.JwtTokenProvider;
import com.jachwisunbae.common.exception.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String MEMBER_ID_ATTRIBUTE = "authenticatedMemberId";
    private final JwtTokenProvider provider;
    private final AuthenticationErrorWriter errorWriter;

    public JwtAuthenticationFilter(
            JwtTokenProvider provider,
            AuthenticationErrorWriter errorWriter) {
        this.provider = provider;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        if (isPublicRequest(request)) {
            chain.doFilter(request, response);
            return;
        }

        String authorization = request.getHeader("Authorization");
        if (!isBearerToken(authorization)) {
            errorWriter.write(response);
            return;
        }

        try {
            request.setAttribute(
                    MEMBER_ID_ATTRIBUTE,
                    provider.parseMemberId(extractToken(authorization)));
        } catch (BusinessException exception) {
            errorWriter.write(response);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isPublicRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            return true;
        }
        if ("/api/check-items".equals(path) && "GET".equals(request.getMethod())) {
            return true;
        }
        return path.startsWith("/api/auth/");
    }

    private boolean isBearerToken(String authorization) {
        return authorization != null
                && authorization.startsWith("Bearer ")
                && authorization.length() > "Bearer ".length();
    }

    private String extractToken(String authorization) {
        return authorization.substring("Bearer ".length());
    }
}
