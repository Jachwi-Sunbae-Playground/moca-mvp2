package com.jachwisunbae.visit.controller.dto.request;

import com.jachwisunbae.common.exception.client.InvalidCommandException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.visit.domain.InlineMemo;
import com.jachwisunbae.visit.service.dto.command.UpdateVisitItemMemoCommand;
import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateVisitItemMemoRequest(
        @Schema(maxLength = InlineMemo.MAX_LENGTH, requiredMode = Schema.RequiredMode.REQUIRED) String memo,
        @Schema(minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED) Long expectedMemoVersion
) {

    public UpdateVisitItemMemoCommand toCommand() {
        if (expectedMemoVersion == null || expectedMemoVersion < 0) {
            throw new InvalidCommandException(ErrorCode.INVALID_REQUEST);
        }
        return new UpdateVisitItemMemoCommand(new InlineMemo(memo), expectedMemoVersion);
    }
}
