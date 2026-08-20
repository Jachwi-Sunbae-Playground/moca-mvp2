package com.jachwisunbae.property.controller;

import com.jachwisunbae.common.web.ApiResponse;
import com.jachwisunbae.property.controller.dto.response.SystemMemoItemResponse;
import com.jachwisunbae.property.service.SystemMemoItemService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system-memo-items")
@Tag(name = "System memo items", description = "시스템 기본 메모 항목 조회 API")
@SecurityRequirement(name = "bearerAuth")
public class SystemMemoItemController {
    private final SystemMemoItemService systemMemoItemService;

    public SystemMemoItemController(final SystemMemoItemService systemMemoItemService) {
        this.systemMemoItemService = systemMemoItemService;
    }

    @GetMapping
    @Operation(summary = "시스템 메모 항목 조회", description = "매물 메모 초기 생성에 사용하는 활성 시스템 메모 항목을 조회합니다.")
    public ApiResponse<List<SystemMemoItemResponse>> findActive() {
        return ApiResponse.of(systemMemoItemService.findActive().stream()
                .map(SystemMemoItemResponse::from)
                .toList());
    }
}
