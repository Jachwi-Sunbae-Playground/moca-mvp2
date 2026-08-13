package com.jachwisunbae.checklist.domain;

import com.jachwisunbae.common.exception.client.InvalidCommandException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;

public enum CheckStage {

    ONLINE_PHONE,
    ON_SITE,
    PRE_CONTRACT;

    public static CheckStage from(final String value) {
        try {
            return CheckStage.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidCommandException(ErrorCode.INVALID_STAGE);
        }
    }
}
