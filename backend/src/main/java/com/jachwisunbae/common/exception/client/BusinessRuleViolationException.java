package com.jachwisunbae.common.exception.client;

import com.jachwisunbae.common.exception.errorcode.ErrorCode;

public class BusinessRuleViolationException extends ClientException {

    public BusinessRuleViolationException(final ErrorCode errorCode) {
        super(errorCode);
    }
}
