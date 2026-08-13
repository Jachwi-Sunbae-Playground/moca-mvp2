package com.jachwisunbae.property.controller.dto.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.jachwisunbae.common.exception.client.InvalidCommandException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.property.domain.PreVisitMemoField;
import com.jachwisunbae.property.domain.PropertyMemo;
import com.jachwisunbae.property.service.dto.command.SavePropertyMemoCommand;
import io.swagger.v3.oas.annotations.media.Schema;

public final class SavePropertyMemoRequest {

    private String viewingSchedule;
    private String moveInAvailability;
    private String provisionalDeposit;
    private String roomOptions;
    private String maintenanceAndUtilities;
    private String commuteTime;
    private String governmentSupport;
    private String additionalMemo;
    private String content;
    private boolean viewingSchedulePresent;
    private boolean moveInAvailabilityPresent;
    private boolean provisionalDepositPresent;
    private boolean roomOptionsPresent;
    private boolean maintenanceAndUtilitiesPresent;
    private boolean commuteTimePresent;
    private boolean governmentSupportPresent;
    private boolean additionalMemoPresent;
    private boolean contentPresent;

    @JsonSetter("viewingSchedule")
    public void setViewingSchedule(final String viewingSchedule) {
        this.viewingSchedule = viewingSchedule;
        this.viewingSchedulePresent = true;
    }

    @JsonSetter("moveInAvailability")
    public void setMoveInAvailability(final String moveInAvailability) {
        this.moveInAvailability = moveInAvailability;
        this.moveInAvailabilityPresent = true;
    }

    @JsonSetter("provisionalDeposit")
    public void setProvisionalDeposit(final String provisionalDeposit) {
        this.provisionalDeposit = provisionalDeposit;
        this.provisionalDepositPresent = true;
    }

    @JsonSetter("roomOptions")
    public void setRoomOptions(final String roomOptions) {
        this.roomOptions = roomOptions;
        this.roomOptionsPresent = true;
    }

    @JsonSetter("maintenanceAndUtilities")
    public void setMaintenanceAndUtilities(final String maintenanceAndUtilities) {
        this.maintenanceAndUtilities = maintenanceAndUtilities;
        this.maintenanceAndUtilitiesPresent = true;
    }

    @JsonSetter("commuteTime")
    public void setCommuteTime(final String commuteTime) {
        this.commuteTime = commuteTime;
        this.commuteTimePresent = true;
    }

    @JsonSetter("governmentSupport")
    public void setGovernmentSupport(final String governmentSupport) {
        this.governmentSupport = governmentSupport;
        this.governmentSupportPresent = true;
    }

    @JsonSetter("additionalMemo")
    public void setAdditionalMemo(final String additionalMemo) {
        this.additionalMemo = additionalMemo;
        this.additionalMemoPresent = true;
    }

    @JsonSetter("content")
    public void setContent(final String content) {
        this.content = content;
        this.contentPresent = true;
    }

    @Schema(example = "8월 20일 오후 2시 방문", maxLength = PreVisitMemoField.MAX_LENGTH)
    public String getViewingSchedule() {
        return viewingSchedule;
    }

    @Schema(example = "9월 1일부터 입주 가능", maxLength = PreVisitMemoField.MAX_LENGTH)
    public String getMoveInAvailability() {
        return moveInAvailability;
    }

    @Schema(example = "가계약금 30만 원", maxLength = PreVisitMemoField.MAX_LENGTH)
    public String getProvisionalDeposit() {
        return provisionalDeposit;
    }

    @Schema(example = "냉장고와 세탁기 포함", maxLength = PreVisitMemoField.MAX_LENGTH)
    public String getRoomOptions() {
        return roomOptions;
    }

    @Schema(example = "관리비와 전기·가스 별도", maxLength = PreVisitMemoField.MAX_LENGTH)
    public String getMaintenanceAndUtilities() {
        return maintenanceAndUtilities;
    }

    @Schema(example = "학교까지 버스로 20분", maxLength = PreVisitMemoField.MAX_LENGTH)
    public String getCommuteTime() {
        return commuteTime;
    }

    @Schema(example = "중소기업 청년 대출 가능 여부 확인", maxLength = PreVisitMemoField.MAX_LENGTH)
    public String getGovernmentSupport() {
        return governmentSupport;
    }

    @Schema(example = "채광과 골목 소음을 다시 확인", maxLength = PropertyMemo.MAX_LENGTH)
    public String getAdditionalMemo() {
        return additionalMemo;
    }

    @Schema(
            description = "v1.0 호환용 추가 메모. 구조화 필드와 함께 보낼 수 없다.",
            example = "채광과 골목 소음을 다시 확인",
            maxLength = PropertyMemo.MAX_LENGTH,
            deprecated = true
    )
    public String getContent() {
        return content;
    }

    public SavePropertyMemoCommand toCommand() {
        if (contentPresent && isAnyStructuredFieldPresent()) {
            throw new InvalidCommandException(ErrorCode.AMBIGUOUS_MEMO_CONTENT);
        }
        if (contentPresent) {
            return SavePropertyMemoCommand.legacy(content);
        }
        if (!areAllStructuredFieldsPresent()) {
            throw new InvalidCommandException(ErrorCode.PROPERTY_MEMO_INVALID);
        }
        return SavePropertyMemoCommand.replace(
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

    private boolean isAnyStructuredFieldPresent() {
        return viewingSchedulePresent
                || moveInAvailabilityPresent
                || provisionalDepositPresent
                || roomOptionsPresent
                || maintenanceAndUtilitiesPresent
                || commuteTimePresent
                || governmentSupportPresent
                || additionalMemoPresent;
    }

    private boolean areAllStructuredFieldsPresent() {
        return viewingSchedulePresent
                && moveInAvailabilityPresent
                && provisionalDepositPresent
                && roomOptionsPresent
                && maintenanceAndUtilitiesPresent
                && commuteTimePresent
                && governmentSupportPresent
                && additionalMemoPresent;
    }
}
