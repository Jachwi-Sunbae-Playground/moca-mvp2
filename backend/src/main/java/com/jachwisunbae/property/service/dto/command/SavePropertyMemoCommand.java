package com.jachwisunbae.property.service.dto.command;

import com.jachwisunbae.common.exception.client.InvalidCommandException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.property.domain.PreVisitMemoField;
import com.jachwisunbae.property.domain.PropertyMemo;

public final class SavePropertyMemoCommand {

    private final boolean legacy;
    private final String viewingSchedule;
    private final String moveInAvailability;
    private final String provisionalDeposit;
    private final String roomOptions;
    private final String maintenanceAndUtilities;
    private final String commuteTime;
    private final String governmentSupport;
    private final String additionalMemo;

    private SavePropertyMemoCommand(
            final boolean legacy,
            final String viewingSchedule,
            final String moveInAvailability,
            final String provisionalDeposit,
            final String roomOptions,
            final String maintenanceAndUtilities,
            final String commuteTime,
            final String governmentSupport,
            final String additionalMemo
    ) {
        this.legacy = legacy;
        this.viewingSchedule = viewingSchedule;
        this.moveInAvailability = moveInAvailability;
        this.provisionalDeposit = provisionalDeposit;
        this.roomOptions = roomOptions;
        this.maintenanceAndUtilities = maintenanceAndUtilities;
        this.commuteTime = commuteTime;
        this.governmentSupport = governmentSupport;
        this.additionalMemo = additionalMemo;
        validate();
    }

    public static SavePropertyMemoCommand legacy(final String content) {
        return new SavePropertyMemoCommand(true, null, null, null, null, null, null, null, content);
    }

    public static SavePropertyMemoCommand replace(
            final String viewingSchedule,
            final String moveInAvailability,
            final String provisionalDeposit,
            final String roomOptions,
            final String maintenanceAndUtilities,
            final String commuteTime,
            final String governmentSupport,
            final String additionalMemo
    ) {
        return new SavePropertyMemoCommand(
                false,
                viewingSchedule,
                moveInAvailability,
                provisionalDeposit,
                roomOptions,
                maintenanceAndUtilities,
                commuteTime,
                governmentSupport,
                additionalMemo
        );
    }

    private void validate() {
        if (!PropertyMemo.isValid(additionalMemo)) {
            throw invalidMemo();
        }
        if (legacy) {
            return;
        }
        if (!PreVisitMemoField.isValid(viewingSchedule)
                || !PreVisitMemoField.isValid(moveInAvailability)
                || !PreVisitMemoField.isValid(provisionalDeposit)
                || !PreVisitMemoField.isValid(roomOptions)
                || !PreVisitMemoField.isValid(maintenanceAndUtilities)
                || !PreVisitMemoField.isValid(commuteTime)
                || !PreVisitMemoField.isValid(governmentSupport)) {
            throw invalidMemo();
        }
    }

    private InvalidCommandException invalidMemo() {
        return new InvalidCommandException(ErrorCode.PROPERTY_MEMO_INVALID);
    }

    public boolean isLegacy() {
        return legacy;
    }

    public String viewingSchedule() {
        return viewingSchedule;
    }

    public String moveInAvailability() {
        return moveInAvailability;
    }

    public String provisionalDeposit() {
        return provisionalDeposit;
    }

    public String roomOptions() {
        return roomOptions;
    }

    public String maintenanceAndUtilities() {
        return maintenanceAndUtilities;
    }

    public String commuteTime() {
        return commuteTime;
    }

    public String governmentSupport() {
        return governmentSupport;
    }

    public String additionalMemo() {
        return additionalMemo;
    }
}
