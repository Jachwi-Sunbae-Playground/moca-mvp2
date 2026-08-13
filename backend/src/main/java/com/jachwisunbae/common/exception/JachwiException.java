package com.jachwisunbae.common.exception;

import com.jachwisunbae.common.exception.errorcode.ErrorCode;

public abstract class JachwiException extends RuntimeException {

    private final ErrorCode errorCode;

    protected JachwiException(final ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    protected JachwiException(final ErrorCode errorCode, final Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
