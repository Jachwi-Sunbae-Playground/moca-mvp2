package com.jachwisunbae.common.exception.server;

import com.jachwisunbae.common.exception.errorcode.ErrorCode;

public class UpstreamServiceException extends ServerException {

    public UpstreamServiceException(final ErrorCode errorCode) {
        super(errorCode);
    }

    public UpstreamServiceException(final ErrorCode errorCode, final Throwable cause) {
        super(errorCode, cause);
    }
}
