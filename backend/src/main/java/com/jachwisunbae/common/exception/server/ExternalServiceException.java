package com.jachwisunbae.common.exception.server;

import com.jachwisunbae.common.exception.errorcode.ErrorCode;

public class ExternalServiceException extends ServerException {

    public ExternalServiceException(final ErrorCode errorCode) {
        super(errorCode);
    }

    public ExternalServiceException(final ErrorCode errorCode, final Throwable cause) {
        super(errorCode, cause);
    }
}
