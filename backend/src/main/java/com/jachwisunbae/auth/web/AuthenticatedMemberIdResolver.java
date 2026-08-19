package com.jachwisunbae.auth.web;

import com.jachwisunbae.common.exception.BusinessException;
import com.jachwisunbae.common.exception.DomainErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class AuthenticatedMemberIdResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthenticatedMemberId.class)
                && parameter.getParameterType() == Long.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
            NativeWebRequest request, WebDataBinderFactory factory) {
        Object memberId = request.getAttribute(JwtAuthenticationFilter.MEMBER_ID_ATTRIBUTE, 0);
        if (memberId == null) {
            throw new BusinessException(
                    DomainErrorCode.ACCESS_TOKEN_INVALID,
                    "Access Token이 필요합니다.");
        }
        return memberId;
    }
}
