package com.jachwisunbae.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final DomainErrorCode code;

    public BusinessException(DomainErrorCode code, String debugMessage) {
        super(debugMessage);
        this.code = code;
    }

    public BusinessException(DomainErrorCode code, String debugMessage, Throwable cause) {
        super(debugMessage, cause);
        this.code = code;
    }
}
