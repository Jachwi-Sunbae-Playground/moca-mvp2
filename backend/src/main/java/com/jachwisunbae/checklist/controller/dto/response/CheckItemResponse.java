package com.jachwisunbae.checklist.controller.dto.response;

import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.checklist.service.dto.result.CheckItemResult;

public record CheckItemResponse(
        long checkItemId,
        CheckStage stage,
        String question,
        String guide
) {

    public static CheckItemResponse from(final CheckItemResult result) {
        return new CheckItemResponse(
                result.checkItemId(),
                result.stage(),
                result.question(),
                result.guide()
        );
    }
}
