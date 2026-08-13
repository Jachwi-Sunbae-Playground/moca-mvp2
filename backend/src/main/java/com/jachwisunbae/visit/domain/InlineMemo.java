package com.jachwisunbae.visit.domain;

import com.jachwisunbae.common.exception.client.InvalidCommandException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;

public record InlineMemo(String value) {

    public static final int MAX_LENGTH = 200;

    public InlineMemo {
        if (value == null
                || value.codePointCount(0, value.length()) > MAX_LENGTH
                || value.indexOf('\r') >= 0
                || value.indexOf('\n') >= 0) {
            throw new InvalidCommandException(ErrorCode.VISIT_ITEM_MEMO_INVALID);
        }
    }
}
