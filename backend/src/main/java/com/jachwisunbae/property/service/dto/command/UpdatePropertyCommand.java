package com.jachwisunbae.property.service.dto.command;

import com.jachwisunbae.common.exception.client.InvalidCommandException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import java.util.Optional;

public record UpdatePropertyCommand(
        Optional<String> name,
        Optional<Long> depositAmount,
        Optional<Long> monthlyRentAmount,
        Optional<String> discoverySource
) {

    public UpdatePropertyCommand {
        name = name == null ? Optional.empty() : name;
        depositAmount = depositAmount == null ? Optional.empty() : depositAmount;
        monthlyRentAmount = monthlyRentAmount == null ? Optional.empty() : monthlyRentAmount;
        discoverySource = discoverySource == null ? Optional.empty() : discoverySource;
        if (name.isEmpty()
                && depositAmount.isEmpty()
                && monthlyRentAmount.isEmpty()
                && discoverySource.isEmpty()) {
            throw new InvalidCommandException(ErrorCode.INVALID_REQUEST);
        }
    }
}
