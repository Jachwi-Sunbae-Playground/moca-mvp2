package com.jachwisunbae.checklist.controller.dto.response;

import com.jachwisunbae.checklist.service.dto.result.OrderedCheckItemResult;

public record OrderedCheckItemResponse(
        long checkItemId,
        String question,
        String guide,
        int order
) {

    public static OrderedCheckItemResponse from(final OrderedCheckItemResult result) {
        return new OrderedCheckItemResponse(
                result.checkItemId(),
                result.question(),
                result.guide(),
                result.order()
        );
    }
}
