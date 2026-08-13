package com.jachwisunbae.visit.controller.dto.request;

import com.jachwisunbae.visit.service.dto.command.CompleteVisitCommand;
import jakarta.validation.constraints.NotBlank;

public record CompleteVisitRequest(@NotBlank String status) {

    public CompleteVisitCommand toCommand() {
        return new CompleteVisitCommand(status);
    }
}
