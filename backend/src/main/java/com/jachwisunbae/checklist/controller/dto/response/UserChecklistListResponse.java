package com.jachwisunbae.checklist.controller.dto.response;

import java.util.List;

public record UserChecklistListResponse(int totalCount, List<UserChecklistSummaryResponse> items) {
}
