package com.jachwisunbae.common.page;

import com.jachwisunbae.common.exception.client.InvalidCommandException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;

public record PageQuery(int page, int size) {

    private static final int MAX_SIZE = 100;

    public static PageQuery of(final int page, final int size) {
        if (page < 0 || size < 1 || size > MAX_SIZE) {
            throw new InvalidCommandException(ErrorCode.INVALID_PAGE_REQUEST);
        }
        return new PageQuery(page, size);
    }

    public long offset() {
        return (long) page * size;
    }
}
