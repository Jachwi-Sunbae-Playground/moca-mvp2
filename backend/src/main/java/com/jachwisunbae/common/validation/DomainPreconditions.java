package com.jachwisunbae.common.validation;

import com.jachwisunbae.common.exception.BusinessException;
import com.jachwisunbae.common.exception.DomainErrorCode;

public final class DomainPreconditions {

    private DomainPreconditions() {
    }

    public static <T> T requireNonNull(
            T value,
            DomainErrorCode code,
            String debugMessage) {
        if (value == null) {
            throw new BusinessException(code, debugMessage);
        }
        return value;
    }

    public static String requireNonBlank(
            String value,
            DomainErrorCode code,
            String debugMessage) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(code, debugMessage);
        }
        return value;
    }

    public static String requireTrimmed(
            String value,
            int minLength,
            int maxLength,
            DomainErrorCode code,
            String debugMessage) {
        String trimmed = requireNonBlank(value, code, debugMessage).trim();
        require(trimmed.length() >= minLength && trimmed.length() <= maxLength, code, debugMessage);
        return trimmed;
    }

    public static long requireNonNegative(
            Long value,
            DomainErrorCode code,
            String debugMessage) {
        requireNonNull(value, code, debugMessage);
        require(value >= 0, code, debugMessage);
        return value;
    }

    public static long requireAtMost(
            Long value,
            long maximum,
            DomainErrorCode code,
            String debugMessage) {
        requireNonNull(value, code, debugMessage);
        require(value <= maximum, code, debugMessage);
        return value;
    }

    public static int requirePositive(
            Integer value,
            DomainErrorCode code,
            String debugMessage) {
        requireNonNull(value, code, debugMessage);
        require(value > 0, code, debugMessage);
        return value;
    }

    public static void require(
            boolean condition,
            DomainErrorCode code,
            String debugMessage) {
        if (!condition) {
            throw new BusinessException(code, debugMessage);
        }
    }
}
