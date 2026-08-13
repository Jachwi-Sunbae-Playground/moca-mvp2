package com.jachwisunbae.common.exception.client;

import com.jachwisunbae.common.exception.errorcode.ErrorCode;

public class AuthenticationFailedException extends ClientException {

    public AuthenticationFailedException(final ErrorCode errorCode) {
        super(errorCode);
    }
}
