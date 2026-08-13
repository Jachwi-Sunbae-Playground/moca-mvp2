package com.jachwisunbae.common.exception.client;

import com.jachwisunbae.common.exception.errorcode.ErrorCode;

public class ResourceNotFoundException extends ClientException {

    public ResourceNotFoundException(final ErrorCode errorCode) {
        super(errorCode);
    }
}
