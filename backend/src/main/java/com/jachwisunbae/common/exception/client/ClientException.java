package com.jachwisunbae.common.exception.client;

import com.jachwisunbae.common.exception.JachwiException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;

public class ClientException extends JachwiException {

    public ClientException(final ErrorCode errorCode) {
        super(errorCode);
    }

    public ClientException(final ErrorCode errorCode, final Throwable cause) {
        super(errorCode, cause);
    }
}
