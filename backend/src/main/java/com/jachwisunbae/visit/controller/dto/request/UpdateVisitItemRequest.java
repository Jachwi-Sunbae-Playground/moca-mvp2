package com.jachwisunbae.visit.controller.dto.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.jachwisunbae.common.exception.client.InvalidCommandException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.visit.service.dto.command.UpdateVisitItemStatusCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;

public final class UpdateVisitItemRequest {

    private String status;
    private Long expectedStatusVersion;
    private Long expectedVersion;
    private boolean expectedStatusVersionPresent;
    private boolean expectedVersionPresent;

    public void setStatus(final String status) {
        this.status = status;
    }

    @JsonSetter("expectedStatusVersion")
    public void setExpectedStatusVersion(final Long expectedStatusVersion) {
        this.expectedStatusVersion = expectedStatusVersion;
        this.expectedStatusVersionPresent = true;
    }

    @JsonSetter("expectedVersion")
    public void setExpectedVersion(final Long expectedVersion) {
        this.expectedVersion = expectedVersion;
        this.expectedVersionPresent = true;
    }

    @NotBlank
    public String getStatus() {
        return status;
    }

    @Schema(description = "v1.1 상태 CAS 버전", minimum = "0")
    public Long getExpectedStatusVersion() {
        return expectedStatusVersion;
    }

    @Schema(description = "v1.0 호환 상태 CAS 버전", minimum = "0", deprecated = true)
    public Long getExpectedVersion() {
        return expectedVersion;
    }

    public UpdateVisitItemStatusCommand toCommand() {
        if (expectedStatusVersionPresent
                && expectedVersionPresent
                && !Objects.equals(expectedStatusVersion, expectedVersion)) {
            throw new InvalidCommandException(ErrorCode.AMBIGUOUS_STATUS_VERSION);
        }
        final Long normalizedVersion = expectedStatusVersionPresent ? expectedStatusVersion : expectedVersion;
        if ((!expectedStatusVersionPresent && !expectedVersionPresent)
                || normalizedVersion == null
                || normalizedVersion < 0) {
            throw new InvalidCommandException(ErrorCode.INVALID_REQUEST);
        }
        return new UpdateVisitItemStatusCommand(status, normalizedVersion);
    }
}
