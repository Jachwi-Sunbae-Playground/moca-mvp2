package com.jachwisunbae.visit.domain;

import com.jachwisunbae.common.exception.client.InvalidCommandException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;

public enum CheckStatus {

    GOOD,
    CAUTION,
    UNCONFIRMED;

    public static CheckStatus from(final String value) {
        try {
            return CheckStatus.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidCommandException(ErrorCode.INVALID_CHECK_STATUS);
        }
    }
}
