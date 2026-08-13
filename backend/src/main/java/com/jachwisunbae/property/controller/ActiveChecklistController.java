package com.jachwisunbae.property.controller;

import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.common.config.OpenApiConfig;
import com.jachwisunbae.common.resolver.AuthenticatedMemberId;
import com.jachwisunbae.common.response.ApiResponse;
import com.jachwisunbae.common.response.ErrorResponse;
import com.jachwisunbae.property.controller.dto.request.AssignActiveChecklistRequest;
import com.jachwisunbae.property.controller.dto.response.ActiveChecklistResponse;
import com.jachwisunbae.property.service.ActiveChecklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/properties/{propertyId}/active-checklists")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class ActiveChecklistController {

    private final ActiveChecklistService activeChecklistService;

    public ActiveChecklistController(final ActiveChecklistService activeChecklistService) {
        this.activeChecklistService = activeChecklistService;
    }

    @Operation(summary = "활성 체크리스트 설정·교체", description = "매물 행을 잠그고 같은 단계의 소유 체크리스트를 원자적으로 연결한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "설정·교체 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 단계 또는 체크리스트 단계 불일치",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "매물·체크리스트가 없거나 다른 회원이 소유함",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PutMapping("/{stage}")
    public ResponseEntity<ApiResponse<ActiveChecklistResponse>> assign(
            @AuthenticatedMemberId final long memberId,
            @Positive @PathVariable final long propertyId,
            @PathVariable final String stage,
            @Valid @RequestBody final AssignActiveChecklistRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(ActiveChecklistResponse.from(
                activeChecklistService.assign(
                        memberId,
                        propertyId,
                        CheckStage.from(stage),
                        request.checklistId()
                )
        )));
    }

    @Operation(summary = "활성 체크리스트 연결 해제", description = "체크리스트는 유지하고 매물의 해당 단계 연결만 멱등하게 해제한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "연결 해제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 단계",
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
    @DeleteMapping("/{stage}")
    public ResponseEntity<Void> unassign(
            @AuthenticatedMemberId final long memberId,
            @Positive @PathVariable final long propertyId,
            @PathVariable final String stage
    ) {
        activeChecklistService.unassign(memberId, propertyId, CheckStage.from(stage));
        return ResponseEntity.noContent().build();
    }
}
