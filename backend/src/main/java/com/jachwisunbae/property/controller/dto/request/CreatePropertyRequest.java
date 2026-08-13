package com.jachwisunbae.property.controller.dto.request;

import com.jachwisunbae.property.domain.Money;
import com.jachwisunbae.property.service.dto.command.CreatePropertyCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.hibernate.validator.constraints.CodePointLength;

public record CreatePropertyRequest(
        @Schema(example = "신림역 원룸", maxLength = 50)
        @NotBlank(message = "공백을 제외하고 1자 이상 입력해야 합니다.")
        @CodePointLength(max = 50, message = "50자 이하로 입력해야 합니다.")
        String name,

        @Schema(example = "10000000", minimum = "0", maximum = "9007199254740991")
        @NotNull(message = "보증금을 입력해야 합니다.")
        @PositiveOrZero(message = "0 이상의 금액을 입력해야 합니다.")
        @Max(value = Money.MAX_AMOUNT, message = "허용된 최대 금액 이하로 입력해야 합니다.")
        Long depositAmount,

        @Schema(example = "550000", minimum = "0", maximum = "9007199254740991")
        @NotNull(message = "월세를 입력해야 합니다.")
        @PositiveOrZero(message = "0 이상의 금액을 입력해야 합니다.")
        @Max(value = Money.MAX_AMOUNT, message = "허용된 최대 금액 이하로 입력해야 합니다.")
        Long monthlyRentAmount,

        @Schema(example = "https://example.com/listings/10", maxLength = 500)
        @NotBlank(message = "공백을 제외하고 1자 이상 입력해야 합니다.")
        @CodePointLength(max = 500, message = "500자 이하로 입력해야 합니다.")
        String discoverySource
) {

    public CreatePropertyRequest {
        name = name == null ? null : name.trim();
        discoverySource = discoverySource == null ? null : discoverySource.trim();
    }

    public CreatePropertyCommand toCommand() {
        return new CreatePropertyCommand(name, depositAmount, monthlyRentAmount, discoverySource);
    }
}
