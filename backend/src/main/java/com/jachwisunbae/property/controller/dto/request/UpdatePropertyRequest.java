package com.jachwisunbae.property.controller.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.jachwisunbae.property.domain.Money;
import com.jachwisunbae.property.service.dto.command.UpdatePropertyCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.Optional;
import org.hibernate.validator.constraints.CodePointLength;

public final class UpdatePropertyRequest {

    private String name;
    private Long depositAmount;
    private Long monthlyRentAmount;
    private String discoverySource;
    private boolean namePresent;
    private boolean depositAmountPresent;
    private boolean monthlyRentAmountPresent;
    private boolean discoverySourcePresent;

    @JsonSetter(value = "name", nulls = Nulls.FAIL)
    public void setName(final String name) {
        this.name = name.trim();
        this.namePresent = true;
    }

    @JsonSetter(value = "depositAmount", nulls = Nulls.FAIL)
    public void setDepositAmount(final Long depositAmount) {
        this.depositAmount = depositAmount;
        this.depositAmountPresent = true;
    }

    @JsonSetter(value = "monthlyRentAmount", nulls = Nulls.FAIL)
    public void setMonthlyRentAmount(final Long monthlyRentAmount) {
        this.monthlyRentAmount = monthlyRentAmount;
        this.monthlyRentAmountPresent = true;
    }

    @JsonSetter(value = "discoverySource", nulls = Nulls.FAIL)
    public void setDiscoverySource(final String discoverySource) {
        this.discoverySource = discoverySource.trim();
        this.discoverySourcePresent = true;
    }

    @Schema(example = "신림역 원룸 2차 방문", maxLength = 50)
    @Pattern(regexp = "(?s).*\\S.*", message = "공백을 제외하고 1자 이상 입력해야 합니다.")
    @CodePointLength(max = 50, message = "50자 이하로 입력해야 합니다.")
    public String getName() {
        return name;
    }

    @Schema(example = "10000000", minimum = "0", maximum = "9007199254740991")
    @PositiveOrZero(message = "0 이상의 금액을 입력해야 합니다.")
    @Max(value = Money.MAX_AMOUNT, message = "허용된 최대 금액 이하로 입력해야 합니다.")
    public Long getDepositAmount() {
        return depositAmount;
    }

    @Schema(example = "530000", minimum = "0", maximum = "9007199254740991")
    @PositiveOrZero(message = "0 이상의 금액을 입력해야 합니다.")
    @Max(value = Money.MAX_AMOUNT, message = "허용된 최대 금액 이하로 입력해야 합니다.")
    public Long getMonthlyRentAmount() {
        return monthlyRentAmount;
    }

    @Schema(example = "직방 앱에서 발견", maxLength = 500)
    @Pattern(regexp = "(?s).*\\S.*", message = "공백을 제외하고 1자 이상 입력해야 합니다.")
    @CodePointLength(max = 500, message = "500자 이하로 입력해야 합니다.")
    public String getDiscoverySource() {
        return discoverySource;
    }

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "변경할 필드를 하나 이상 입력해야 합니다.")
    public boolean isAnyFieldPresent() {
        return namePresent || depositAmountPresent || monthlyRentAmountPresent || discoverySourcePresent;
    }

    public UpdatePropertyCommand toCommand() {
        return new UpdatePropertyCommand(
                namePresent ? Optional.of(name) : Optional.empty(),
                depositAmountPresent ? Optional.of(depositAmount) : Optional.empty(),
                monthlyRentAmountPresent ? Optional.of(monthlyRentAmount) : Optional.empty(),
                discoverySourcePresent ? Optional.of(discoverySource) : Optional.empty()
        );
    }
}
