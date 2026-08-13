package com.jachwisunbae.checklist.domain;

import com.jachwisunbae.common.exception.client.InvalidCommandException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;

public record ChecklistName(String value) {

    private static final int MAX_LENGTH = 50;

    public ChecklistName {
        value = value == null ? null : value.trim();
        if (value == null || value.isEmpty() || value.codePointCount(0, value.length()) > MAX_LENGTH) {
            throw new InvalidCommandException(ErrorCode.INVALID_REQUEST);
        }
    }
}
