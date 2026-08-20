package com.jachwisunbae.checklist.controller;

import com.jachwisunbae.checklist.controller.dto.request.SystemCheckItemSearchRequest;
import com.jachwisunbae.checklist.controller.dto.response.SystemCheckItemResponse;
import com.jachwisunbae.checklist.service.SystemCheckItemService;
import com.jachwisunbae.common.web.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/check-items")
@Tag(name = "System check items", description = "시스템 체크 항목 조회 API")
public class SystemCheckItemController {

    private final SystemCheckItemService systemCheckItemService;

    public SystemCheckItemController(final SystemCheckItemService systemCheckItemService) {
        this.systemCheckItemService = systemCheckItemService;
    }

    @GetMapping
    @Operation(summary = "시스템 체크 항목 검색", description = "단계와 선택 검색어로 활성 시스템 체크 항목을 조회합니다.")
    public ApiResponse<List<SystemCheckItemResponse>> search(
            @Valid @ModelAttribute final SystemCheckItemSearchRequest request) {
        List<SystemCheckItemResponse> response = systemCheckItemService.search(request.stage(), request.query())
                .stream()
                .map(SystemCheckItemResponse::from)
                .toList();

        return ApiResponse.of(response);
    }
}
