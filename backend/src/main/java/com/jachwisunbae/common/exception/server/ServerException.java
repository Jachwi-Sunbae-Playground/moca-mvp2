package com.jachwisunbae.common.exception.server;

import com.jachwisunbae.common.exception.JachwiException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;

public class ServerException extends JachwiException {

    public ServerException(final ErrorCode errorCode) {
        super(errorCode);
    }

    public ServerException(final ErrorCode errorCode, final Throwable cause) {
        super(errorCode, cause);
    }
}
