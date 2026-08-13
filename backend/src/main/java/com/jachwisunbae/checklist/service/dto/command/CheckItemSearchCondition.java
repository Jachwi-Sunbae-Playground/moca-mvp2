package com.jachwisunbae.checklist.service.dto.command;

import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.common.exception.client.InvalidCommandException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.page.PageQuery;
import java.util.Objects;

public record CheckItemSearchCondition(
        CheckStage stage,
        String query,
        PageQuery pageQuery
) {

    private static final int MAX_QUERY_LENGTH = 500;

    public CheckItemSearchCondition {
        Objects.requireNonNull(stage);
        query = query == null ? "" : query.trim();
        if (query.codePointCount(0, query.length()) > MAX_QUERY_LENGTH) {
            throw new InvalidCommandException(ErrorCode.INVALID_REQUEST);
        }
        Objects.requireNonNull(pageQuery);
    }
}
