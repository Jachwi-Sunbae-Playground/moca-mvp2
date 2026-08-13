package com.jachwisunbae.checklist.domain;

import com.jachwisunbae.common.exception.client.ResourceNotFoundException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;

public enum ChecklistPresetType {

    ONE_ROOM,
    GOSHIWON;

    public static ChecklistPresetType from(final String value) {
        try {
            return ChecklistPresetType.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ResourceNotFoundException(ErrorCode.CHECKLIST_PRESET_NOT_FOUND);
        }
    }
}
