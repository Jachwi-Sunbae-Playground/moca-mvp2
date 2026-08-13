package com.jachwisunbae.visit.domain;

import com.jachwisunbae.common.exception.client.InvalidCommandException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;

public enum VisitStatus {

    IN_PROGRESS,
    COMPLETED;

    public static VisitStatus completionFrom(final String value) {
        if (COMPLETED.name().equals(value)) {
            return COMPLETED;
        }
        throw new InvalidCommandException(ErrorCode.INVALID_VISIT_STATUS);
    }
}
