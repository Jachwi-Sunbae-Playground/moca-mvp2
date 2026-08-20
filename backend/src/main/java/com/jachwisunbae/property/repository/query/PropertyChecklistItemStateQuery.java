package com.jachwisunbae.property.repository.query;

import com.jachwisunbae.checklist.type.CheckStatus;

public record PropertyChecklistItemStateQuery(Long systemCheckItemId, String question, Integer displayOrder,
                                               CheckStatus status, String memo) {
}
