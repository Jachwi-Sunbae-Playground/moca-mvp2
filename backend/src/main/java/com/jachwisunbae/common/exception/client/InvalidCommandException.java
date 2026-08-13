package com.jachwisunbae.common.exception.client;

import com.jachwisunbae.common.exception.errorcode.ErrorCode;

public class InvalidCommandException extends ClientException {

    public InvalidCommandException(final ErrorCode errorCode) {
        super(errorCode);
    }

    public InvalidCommandException(final ErrorCode errorCode, final Throwable cause) {
        super(errorCode, cause);
    }
}
