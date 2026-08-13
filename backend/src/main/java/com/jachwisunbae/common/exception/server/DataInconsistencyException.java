package com.jachwisunbae.common.exception.server;

import com.jachwisunbae.common.exception.errorcode.ErrorCode;

public class DataInconsistencyException extends ServerException {

    public DataInconsistencyException(final ErrorCode errorCode) {
        super(errorCode);
    }

    public DataInconsistencyException(final ErrorCode errorCode, final Throwable cause) {
        super(errorCode, cause);
    }
}
