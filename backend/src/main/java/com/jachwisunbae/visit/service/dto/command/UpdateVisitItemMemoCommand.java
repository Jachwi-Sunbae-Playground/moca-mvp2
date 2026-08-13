package com.jachwisunbae.visit.service.dto.command;

import com.jachwisunbae.visit.domain.InlineMemo;

public record UpdateVisitItemMemoCommand(InlineMemo memo, long expectedMemoVersion) {
}
