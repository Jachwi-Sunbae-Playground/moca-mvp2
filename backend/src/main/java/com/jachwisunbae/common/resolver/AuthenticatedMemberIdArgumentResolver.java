package com.jachwisunbae.common.resolver;

import com.jachwisunbae.common.exception.client.AuthenticationFailedException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class AuthenticatedMemberIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(final MethodParameter parameter) {
        final Class<?> parameterType = parameter.getParameterType();
        return parameter.hasParameterAnnotation(AuthenticatedMemberId.class)
                && (parameterType == long.class || parameterType == Long.class);
    }

    @Override
    public Object resolveArgument(
            final MethodParameter parameter,
            final ModelAndViewContainer mavContainer,
            final NativeWebRequest webRequest,
            final WebDataBinderFactory binderFactory
    ) {
        final Object principal = SecurityContextHolder.getContext().getAuthentication();
        if (!(principal instanceof JwtAuthenticationToken authenticationToken)) {
            throw new AuthenticationFailedException(ErrorCode.ACCESS_TOKEN_INVALID);
        }
        try {
            final long memberId = Long.parseLong(authenticationToken.getToken().getSubject());
            if (memberId <= 0) {
                throw new NumberFormatException("non-positive subject");
            }
            return memberId;
        } catch (NumberFormatException exception) {
            throw new AuthenticationFailedException(ErrorCode.ACCESS_TOKEN_INVALID);
        }
    }
}
