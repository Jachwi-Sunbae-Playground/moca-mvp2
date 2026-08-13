package com.jachwisunbae.checklist.controller;

import com.jachwisunbae.checklist.controller.dto.request.CreateChecklistRequest;
import com.jachwisunbae.checklist.controller.dto.request.ReplaceChecklistRequest;
import com.jachwisunbae.checklist.controller.dto.response.ChecklistDetailResponse;
import com.jachwisunbae.checklist.controller.dto.response.ChecklistSummaryResponse;
import com.jachwisunbae.checklist.controller.dto.response.CreateChecklistResponse;
import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.checklist.service.ChecklistCommandService;
import com.jachwisunbae.checklist.service.ChecklistQueryService;
import com.jachwisunbae.common.config.OpenApiConfig;
import com.jachwisunbae.common.page.PageQuery;
import com.jachwisunbae.common.page.PageResponse;
import com.jachwisunbae.common.resolver.AuthenticatedMemberId;
import com.jachwisunbae.common.response.ApiResponse;
import com.jachwisunbae.common.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/checklists")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class ChecklistController {

    private final ChecklistQueryService checklistQueryService;
    private final ChecklistCommandService checklistCommandService;

    public ChecklistController(
            final ChecklistQueryService checklistQueryService,
            final ChecklistCommandService checklistCommandService
    ) {
        this.checklistQueryService = checklistQueryService;
        this.checklistCommandService = checklistCommandService;
    }

    @Operation(summary = "내 체크리스트 목록 조회", description = "인증 회원의 단계별 체크리스트를 최근 수정 순으로 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "단계·페이지 요청 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ChecklistSummaryResponse>>> getChecklists(
            @AuthenticatedMemberId final long memberId,
            @RequestParam(required = false) final String stage,
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "20") final int size
    ) {
        final PageResponse<ChecklistSummaryResponse> response = PageResponse.from(
                checklistQueryService.getChecklists(
                        memberId,
                        CheckStage.from(stage),
                        PageQuery.of(page, size)
                ),
                ChecklistSummaryResponse::from
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "내 체크리스트 생성", description = "이름·단계·정렬된 PROVIDED·CUSTOM 항목을 하나의 트랜잭션으로 저장한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "체크리스트 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "이름·항목 구성 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "기준 체크 항목이 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<ApiResponse<CreateChecklistResponse>> createChecklist(
            @AuthenticatedMemberId final long memberId,
            @Valid @RequestBody final CreateChecklistRequest request
    ) {
        final CreateChecklistResponse response = CreateChecklistResponse.from(
                checklistCommandService.createChecklist(memberId, request.toCommand())
        );
        return ResponseEntity.created(URI.create("/api/checklists/" + response.checklistId()))
                .body(ApiResponse.success(response));
    }

    @Operation(summary = "내 체크리스트 상세 조회", description = "소유권과 정렬 순서를 유지해 체크리스트 전체 구성을 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "체크리스트가 없거나 다른 회원이 소유함",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{checklistId}")
    public ResponseEntity<ApiResponse<ChecklistDetailResponse>> getChecklist(
            @AuthenticatedMemberId final long memberId,
            @Positive @PathVariable final long checklistId
    ) {
        return ResponseEntity.ok(ApiResponse.success(ChecklistDetailResponse.from(
                checklistQueryService.getChecklist(memberId, checklistId)
        )));
    }

    @Operation(
            summary = "내 체크리스트 전체 변경",
            description = "단계를 유지하고 기존 로컬 항목 ID를 보존하며 이름·PROVIDED·CUSTOM 항목·순서를 원자적으로 교체한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "체크리스트 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "이름·항목 구성 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "체크리스트·기준 항목을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "CUSTOM 항목이 있어 v1.0 전체 변경 요청을 적용할 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PutMapping("/{checklistId}")
    public ResponseEntity<ApiResponse<ChecklistDetailResponse>> replaceChecklist(
            @AuthenticatedMemberId final long memberId,
            @Positive @PathVariable final long checklistId,
            @Valid @RequestBody final ReplaceChecklistRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(ChecklistDetailResponse.from(
                checklistCommandService.replaceChecklist(memberId, checklistId, request.toCommand())
        )));
    }

    @Operation(summary = "내 체크리스트 삭제", description = "소유 체크리스트·구성 항목과 현재 매물 활성 연결을 물리 삭제한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "체크리스트 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "체크리스트가 없거나 다른 회원이 소유함",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/{checklistId}")
    public ResponseEntity<Void> deleteChecklist(
            @AuthenticatedMemberId final long memberId,
            @Positive @PathVariable final long checklistId
    ) {
        checklistCommandService.deleteChecklist(memberId, checklistId);
        return ResponseEntity.noContent().build();
    }
}
