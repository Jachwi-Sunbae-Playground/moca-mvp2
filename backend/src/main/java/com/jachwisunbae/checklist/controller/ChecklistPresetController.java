package com.jachwisunbae.checklist.controller;

import com.jachwisunbae.checklist.controller.dto.response.ChecklistPresetResponse;
import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.checklist.domain.ChecklistPresetType;
import com.jachwisunbae.checklist.service.CheckCatalogQueryService;
import com.jachwisunbae.common.config.OpenApiConfig;
import com.jachwisunbae.common.response.ApiResponse;
import com.jachwisunbae.common.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checklist-presets")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class ChecklistPresetController {

    private final CheckCatalogQueryService checkCatalogQueryService;

    public ChecklistPresetController(final CheckCatalogQueryService checkCatalogQueryService) {
        this.checkCatalogQueryService = checkCatalogQueryService;
    }

    @Operation(summary = "시작 템플릿 조회", description = "주거 유형과 단계에 맞는 읽기 전용 초기 항목 순서를 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프리셋 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "단계 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "프리셋이 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping
    public ResponseEntity<ApiResponse<ChecklistPresetResponse>> getChecklistPreset(
            @RequestParam(required = false) final String presetType,
            @RequestParam(required = false) final String stage
    ) {
        return ResponseEntity.ok(ApiResponse.success(ChecklistPresetResponse.from(
                checkCatalogQueryService.getChecklistPreset(
                        ChecklistPresetType.from(presetType),
                        CheckStage.from(stage)
                )
        )));
    }
}
