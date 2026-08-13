package com.jachwisunbae.common.security;

import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import java.util.HashSet;
import java.util.Set;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.stereotype.Component;

@Component
public class AccessTokenErrorClassifier {

    public ErrorCode classify(final AuthenticationException exception) {
        final JwtValidationException validationException = findCause(exception, JwtValidationException.class);
        if (validationException != null && validationException.getErrors().stream()
                .anyMatch(error -> AccessTokenValidator.EXPIRED_ERROR_CODE.equals(error.getErrorCode()))) {
            return ErrorCode.ACCESS_TOKEN_EXPIRED;
        }
        if (hasBearerTokenFailure(exception)) {
            return ErrorCode.ACCESS_TOKEN_INVALID;
        }
        return ErrorCode.UNAUTHENTICATED;
    }

    private boolean hasBearerTokenFailure(final Throwable throwable) {
        Throwable current = throwable;
        final Set<Throwable> visited = new HashSet<>();
        while (current != null && visited.add(current)) {
            final String className = current.getClass().getName();
            if (className.contains("BearerToken") || className.contains("Jwt")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private <T extends Throwable> T findCause(final Throwable throwable, final Class<T> targetType) {
        Throwable current = throwable;
        final Set<Throwable> visited = new HashSet<>();
        while (current != null && visited.add(current)) {
            if (targetType.isInstance(current)) {
                return targetType.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }
}
