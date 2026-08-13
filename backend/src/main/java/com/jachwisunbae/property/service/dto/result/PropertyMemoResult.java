package com.jachwisunbae.property.service.dto.result;

import com.jachwisunbae.property.domain.PropertyPreVisitMemo;
import java.time.Instant;

public record PropertyMemoResult(
        String viewingSchedule,
        String moveInAvailability,
        String provisionalDeposit,
        String roomOptions,
        String maintenanceAndUtilities,
        String commuteTime,
        String governmentSupport,
        String additionalMemo,
        Instant savedAt
) {

    public static PropertyMemoResult from(final PropertyPreVisitMemo memo) {
        return new PropertyMemoResult(
                memo.viewingSchedule().value(),
                memo.moveInAvailability().value(),
                memo.provisionalDeposit().value(),
                memo.roomOptions().value(),
                memo.maintenanceAndUtilities().value(),
                memo.commuteTime().value(),
                memo.governmentSupport().value(),
                memo.additionalMemo().content(),
                memo.savedAt()
        );
    }
}
