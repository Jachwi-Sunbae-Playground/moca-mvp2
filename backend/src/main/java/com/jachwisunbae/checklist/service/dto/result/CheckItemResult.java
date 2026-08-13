package com.jachwisunbae.checklist.service.dto.result;

import com.jachwisunbae.checklist.domain.CheckItem;
import com.jachwisunbae.checklist.domain.CheckStage;

public record CheckItemResult(
        long checkItemId,
        CheckStage stage,
        String question,
        String guide
) {

    public static CheckItemResult from(final CheckItem item) {
        return new CheckItemResult(item.id(), item.stage(), item.question(), item.guide());
    }
}
