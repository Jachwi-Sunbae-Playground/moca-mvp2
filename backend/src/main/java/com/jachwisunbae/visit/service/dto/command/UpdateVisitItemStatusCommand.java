package com.jachwisunbae.visit.service.dto.command;

public record UpdateVisitItemStatusCommand(String status, long expectedStatusVersion) {
}
