package com.jachwisunbae.checklist.service.dto.result;

public record OrderedCheckItemResult(
        long checkItemId,
        String question,
        String guide,
        int order
) {
}
