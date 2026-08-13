package com.jachwisunbae.visit.controller;

import com.jachwisunbae.common.config.OpenApiConfig;
import com.jachwisunbae.common.page.PageQuery;
import com.jachwisunbae.common.page.PageResponse;
import com.jachwisunbae.common.resolver.AuthenticatedMemberId;
import com.jachwisunbae.common.response.ApiResponse;
import com.jachwisunbae.common.response.ErrorResponse;
import com.jachwisunbae.visit.controller.dto.request.CompleteVisitRequest;
import com.jachwisunbae.visit.controller.dto.request.UpdateVisitItemMemoRequest;
import com.jachwisunbae.visit.controller.dto.request.UpdateVisitItemRequest;
import com.jachwisunbae.visit.controller.dto.response.VisitCompleteResponse;
import com.jachwisunbae.visit.controller.dto.response.VisitDetailResponse;
import com.jachwisunbae.visit.controller.dto.response.VisitItemMemoResponse;
import com.jachwisunbae.visit.controller.dto.response.VisitItemStatusResponse;
import com.jachwisunbae.visit.controller.dto.response.VisitListItemResponse;
import com.jachwisunbae.visit.service.VisitCommandService;
import com.jachwisunbae.visit.service.VisitQueryService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class VisitController {

    private final VisitQueryService visitQueryService;
    private final VisitCommandService visitCommandService;

    public VisitController(
            final VisitQueryService visitQueryService,
            final VisitCommandService visitCommandService
    ) {
        this.visitQueryService = visitQueryService;
        this.visitCommandService = visitCommandService;
    }

    @Operation(summary = "방문 기록 목록 조회", description = "소유 매물의 방문을 시작 시각 역순으로 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "방문 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "페이지 요청 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "매물이 없거나 다른 회원이 소유함",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/properties/{propertyId}/visits")
    public ResponseEntity<ApiResponse<PageResponse<VisitListItemResponse>>> getVisits(
            @AuthenticatedMemberId final long memberId,
            @Positive @PathVariable final long propertyId,
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "20") final int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(
                visitQueryService.getVisits(memberId, propertyId, PageQuery.of(page, size)),
                VisitListItemResponse::from
        )));
    }

    @Operation(summary = "새 방문 시작", description = "현재 모든 활성 체크리스트를 독립 스냅샷으로 복사한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "방문 시작 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "활성 체크리스트 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "매물이 없거나 다른 회원이 소유함",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "체크리스트 스냅샷 생성 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/properties/{propertyId}/visits")
    public ResponseEntity<ApiResponse<VisitDetailResponse>> startVisit(
            @AuthenticatedMemberId final long memberId,
            @Positive @PathVariable final long propertyId
    ) {
        final VisitDetailResponse response = VisitDetailResponse.from(
                visitCommandService.startVisit(memberId, propertyId)
        );
        return ResponseEntity.created(URI.create("/api/visits/" + response.visitId()))
                .body(ApiResponse.success(response));
    }

    @Operation(summary = "방문 기록 상세 조회", description = "방문 당시 단계·질문·안내·순서 스냅샷과 현재 상태를 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "방문 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "방문이 없거나 다른 회원이 소유함",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/visits/{visitId}")
    public ResponseEntity<ApiResponse<VisitDetailResponse>> getVisit(
            @AuthenticatedMemberId final long memberId,
            @Positive @PathVariable final long visitId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                VisitDetailResponse.from(visitQueryService.getVisit(memberId, visitId))
        ));
    }

    @Operation(summary = "방문 항목 상태 자동 저장", description = "상태 버전 조건으로 상태만 저장하고 메모 채널은 변경하지 않는다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "방문 항목 저장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "상태 또는 버전 요청 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "방문·항목이 없거나 다른 회원이 소유함",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "상태 버전 충돌",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PatchMapping("/visits/{visitId}/items/{visitItemId}")
    public ResponseEntity<ApiResponse<VisitItemStatusResponse>> updateVisitItemStatus(
            @AuthenticatedMemberId final long memberId,
            @Positive @PathVariable final long visitId,
            @Positive @PathVariable final long visitItemId,
            @Valid @RequestBody final UpdateVisitItemRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(VisitItemStatusResponse.from(
                visitCommandService.updateItemStatus(memberId, visitId, visitItemId, request.toCommand())
        )));
    }

    @Operation(summary = "방문 항목 인라인 메모 자동 저장", description = "메모 버전 조건으로 메모만 저장하고 상태 채널은 변경하지 않는다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인라인 메모 저장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "메모 또는 버전 요청 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "방문·항목이 없거나 다른 회원이 소유함",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "메모 버전 충돌",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PatchMapping("/visits/{visitId}/items/{visitItemId}/memo")
    public ResponseEntity<ApiResponse<VisitItemMemoResponse>> updateVisitItemMemo(
            @AuthenticatedMemberId final long memberId,
            @Positive @PathVariable final long visitId,
            @Positive @PathVariable final long visitItemId,
            @Valid @RequestBody final UpdateVisitItemMemoRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(VisitItemMemoResponse.from(
                visitCommandService.updateItemMemo(memberId, visitId, visitItemId, request.toCommand())
        )));
    }

    @Operation(summary = "방문 완료", description = "미확인 항목을 허용하며 최초 완료 시각을 멱등하게 보존한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "방문 완료 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "방문 상태 요청 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "방문이 없거나 다른 회원이 소유함",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PatchMapping("/visits/{visitId}")
    public ResponseEntity<ApiResponse<VisitCompleteResponse>> completeVisit(
            @AuthenticatedMemberId final long memberId,
            @Positive @PathVariable final long visitId,
            @Valid @RequestBody final CompleteVisitRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(VisitCompleteResponse.from(
                visitCommandService.completeVisit(memberId, visitId, request.toCommand())
        )));
    }
}
