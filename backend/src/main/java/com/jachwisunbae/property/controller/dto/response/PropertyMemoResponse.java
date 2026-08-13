package com.jachwisunbae.property.controller.dto.response;

import com.jachwisunbae.property.domain.PreVisitMemoField;
import com.jachwisunbae.property.domain.PropertyMemo;
import com.jachwisunbae.property.service.dto.result.PropertyMemoResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record PropertyMemoResponse(
        @Schema(maxLength = PreVisitMemoField.MAX_LENGTH) String viewingSchedule,
        @Schema(maxLength = PreVisitMemoField.MAX_LENGTH) String moveInAvailability,
        @Schema(maxLength = PreVisitMemoField.MAX_LENGTH) String provisionalDeposit,
        @Schema(maxLength = PreVisitMemoField.MAX_LENGTH) String roomOptions,
        @Schema(maxLength = PreVisitMemoField.MAX_LENGTH) String maintenanceAndUtilities,
        @Schema(maxLength = PreVisitMemoField.MAX_LENGTH) String commuteTime,
        @Schema(maxLength = PreVisitMemoField.MAX_LENGTH) String governmentSupport,
        @Schema(maxLength = PropertyMemo.MAX_LENGTH) String additionalMemo,
        @Schema(
                description = "v1.0 호환용 필드이며 additionalMemo와 항상 같다.",
                maxLength = PropertyMemo.MAX_LENGTH,
                deprecated = true
        ) String content,
        @Schema(nullable = true) Instant savedAt
) {

    public static PropertyMemoResponse from(final PropertyMemoResult result) {
        return new PropertyMemoResponse(
                result.viewingSchedule(),
                result.moveInAvailability(),
                result.provisionalDeposit(),
                result.roomOptions(),
                result.maintenanceAndUtilities(),
                result.commuteTime(),
                result.governmentSupport(),
                result.additionalMemo(),
                result.additionalMemo(),
                result.savedAt()
        );
    }
}
