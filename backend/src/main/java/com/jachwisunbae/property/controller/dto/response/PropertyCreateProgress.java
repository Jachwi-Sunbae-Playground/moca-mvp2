package com.jachwisunbae.property.controller.dto.response;

public record PropertyCreateProgress(int totalCount, int completedCount, int goodCount, int cautionCount,
                                     int unconfirmedCount, int progressRate) {
}
