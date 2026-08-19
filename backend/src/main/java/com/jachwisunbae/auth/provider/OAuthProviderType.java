package com.jachwisunbae.auth.provider;

import com.jachwisunbae.common.exception.BusinessException;
import com.jachwisunbae.common.exception.DomainErrorCode;

public enum OAuthProviderType {
    GOOGLE;

    public static OAuthProviderType from(String value) {
        try {
            return valueOf(value.toUpperCase());
        } catch (Exception exception) {
            throw new BusinessException(DomainErrorCode.OAUTH_PROVIDER_UNSUPPORTED,
                    "지원하지 않는 OAuth 공급자입니다.");
        }
    }
}
