package com.jachwisunbae.checklist.controller;

import com.jachwisunbae.checklist.controller.dto.response.CheckItemResponse;
import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.checklist.service.CheckCatalogQueryService;
import com.jachwisunbae.checklist.service.dto.command.CheckItemSearchCondition;
import com.jachwisunbae.common.config.OpenApiConfig;
import com.jachwisunbae.common.page.PageQuery;
import com.jachwisunbae.common.page.PageResponse;
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
@RequestMapping("/api/check-items")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class CheckItemController {

    private final CheckCatalogQueryService checkCatalogQueryService;

    public CheckItemController(final CheckCatalogQueryService checkCatalogQueryService) {
        this.checkCatalogQueryService = checkCatalogQueryService;
    }

    @Operation(summary = "제공 체크 항목 검색", description = "단계별 활성 기준 항목을 질문 부분 일치로 검색한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "체크 항목 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "단계·검색어·페이지 요청 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CheckItemResponse>>> searchCheckItems(
            @RequestParam(required = false) final String stage,
            @RequestParam(defaultValue = "") final String query,
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "20") final int size
    ) {
        final CheckItemSearchCondition condition = new CheckItemSearchCondition(
                CheckStage.from(stage),
                query,
                PageQuery.of(page, size)
        );
        final PageResponse<CheckItemResponse> response = PageResponse.from(
                checkCatalogQueryService.searchCheckItems(condition),
                CheckItemResponse::from
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
