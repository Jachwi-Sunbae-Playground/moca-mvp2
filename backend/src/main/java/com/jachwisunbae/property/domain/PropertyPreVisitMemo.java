package com.jachwisunbae.property.domain;

import java.time.Instant;
import java.util.Objects;

public record PropertyPreVisitMemo(
        PreVisitMemoField viewingSchedule,
        PreVisitMemoField moveInAvailability,
        PreVisitMemoField provisionalDeposit,
        PreVisitMemoField roomOptions,
        PreVisitMemoField maintenanceAndUtilities,
        PreVisitMemoField commuteTime,
        PreVisitMemoField governmentSupport,
        PropertyMemo additionalMemo,
        Instant savedAt
) {

    public PropertyPreVisitMemo {
        Objects.requireNonNull(viewingSchedule);
        Objects.requireNonNull(moveInAvailability);
        Objects.requireNonNull(provisionalDeposit);
        Objects.requireNonNull(roomOptions);
        Objects.requireNonNull(maintenanceAndUtilities);
        Objects.requireNonNull(commuteTime);
        Objects.requireNonNull(governmentSupport);
        Objects.requireNonNull(additionalMemo);
    }

    public static PropertyPreVisitMemo fallback(
            final PropertyMemo additionalMemo,
            final Instant savedAt
    ) {
        return new PropertyPreVisitMemo(
                PreVisitMemoField.empty(),
                PreVisitMemoField.empty(),
                PreVisitMemoField.empty(),
                PreVisitMemoField.empty(),
                PreVisitMemoField.empty(),
                PreVisitMemoField.empty(),
                PreVisitMemoField.empty(),
                additionalMemo,
                savedAt
        );
    }

    public PropertyPreVisitMemo updateAdditionalMemo(
            final PropertyMemo changedAdditionalMemo,
            final Instant changedSavedAt
    ) {
        return new PropertyPreVisitMemo(
                viewingSchedule,
                moveInAvailability,
                provisionalDeposit,
                roomOptions,
                maintenanceAndUtilities,
                commuteTime,
                governmentSupport,
                changedAdditionalMemo,
                changedSavedAt
        );
    }
}
