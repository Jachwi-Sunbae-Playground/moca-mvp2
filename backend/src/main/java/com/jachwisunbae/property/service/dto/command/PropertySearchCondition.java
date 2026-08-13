package com.jachwisunbae.property.service.dto.command;

import com.jachwisunbae.common.exception.client.InvalidCommandException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.page.PageQuery;

public record PropertySearchCondition(String query, PageQuery pageQuery) {

    private static final int MAX_QUERY_LENGTH = 50;

    public PropertySearchCondition {
        query = query == null ? "" : query.trim();
        if (query.codePointCount(0, query.length()) > MAX_QUERY_LENGTH) {
            throw new InvalidCommandException(ErrorCode.INVALID_REQUEST);
        }
        if (pageQuery == null) {
            throw new InvalidCommandException(ErrorCode.INVALID_PAGE_REQUEST);
        }
    }
}
