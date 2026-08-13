package com.jachwisunbae.property.controller;

import com.jachwisunbae.common.config.OpenApiConfig;
import com.jachwisunbae.common.page.PageQuery;
import com.jachwisunbae.common.page.PageResponse;
import com.jachwisunbae.common.resolver.AuthenticatedMemberId;
import com.jachwisunbae.common.response.ApiResponse;
import com.jachwisunbae.common.response.ErrorResponse;
import com.jachwisunbae.property.controller.dto.request.CreatePropertyRequest;
import com.jachwisunbae.property.controller.dto.request.SavePropertyMemoRequest;
import com.jachwisunbae.property.controller.dto.request.UpdatePropertyRequest;
import com.jachwisunbae.property.controller.dto.response.CreatePropertyResponse;
import com.jachwisunbae.property.controller.dto.response.PropertyDetailResponse;
import com.jachwisunbae.property.controller.dto.response.PropertyMemoResponse;
import com.jachwisunbae.property.controller.dto.response.PropertySummaryResponse;
import com.jachwisunbae.property.controller.dto.response.UpdatePropertyResponse;
import com.jachwisunbae.property.service.PropertyCommandService;
import com.jachwisunbae.property.service.PropertyDeletionService;
import com.jachwisunbae.property.service.PropertyQueryService;
import com.jachwisunbae.property.service.dto.command.PropertySearchCondition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/properties")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class PropertyController {

    private final PropertyQueryService propertyQueryService;
    private final PropertyCommandService propertyCommandService;
    private final PropertyDeletionService propertyDeletionService;

    public PropertyController(
            final PropertyQueryService propertyQueryService,
            final PropertyCommandService propertyCommandService,
            final PropertyDeletionService propertyDeletionService
    ) {
        this.propertyQueryService = propertyQueryService;
        this.propertyCommandService = propertyCommandService;
        this.propertyDeletionService = propertyDeletionService;
    }

    @Operation(summary = "내 매물 목록 조회", description = "인증 회원이 소유한 매물을 이름으로 검색하고 최근 활동 순으로 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "매물 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "검색어 또는 페이지 요청 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PropertySummaryResponse>>> getProperties(
            @AuthenticatedMemberId final long memberId,
            @RequestParam(defaultValue = "") final String query,
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "20") final int size
    ) {
        final PropertySearchCondition condition = new PropertySearchCondition(query, PageQuery.of(page, size));
        final PageResponse<PropertySummaryResponse> response = PageResponse.from(
                propertyQueryService.getProperties(memberId, condition),
                PropertySummaryResponse::from
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "매물 등록", description = "네 필수 정보를 검증하고 인증 회원 소유의 매물을 등록한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "매물 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "필수값 또는 범위 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<ApiResponse<CreatePropertyResponse>> createProperty(
            @AuthenticatedMemberId final long memberId,
            @Valid @RequestBody final CreatePropertyRequest request
    ) {
        final CreatePropertyResponse response = CreatePropertyResponse.from(
                propertyCommandService.createProperty(memberId, request.toCommand())
        );
        final URI location = URI.create("/api/properties/" + response.propertyId());
        return ResponseEntity.created(location).body(ApiResponse.success(response));
    }

    @Operation(summary = "매물 상세 조회", description = "인증 회원이 소유한 매물의 기본 정보와 현재 기록 요약을 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "매물 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "매물이 없거나 다른 회원이 소유함",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{propertyId}")
    public ResponseEntity<ApiResponse<PropertyDetailResponse>> getProperty(
            @AuthenticatedMemberId final long memberId,
            @Parameter(description = "양의 정수 매물 식별자")
            @Positive @PathVariable final long propertyId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                PropertyDetailResponse.from(propertyQueryService.getProperty(memberId, propertyId))
        ));
    }

    @Operation(summary = "매물 기본 정보 변경", description = "전달된 필드만 변경하며 명시적 null과 빈 변경 요청은 거부한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "매물 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "변경 요청 검증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "매물이 없거나 다른 회원이 소유함",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PatchMapping("/{propertyId}")
    public ResponseEntity<ApiResponse<UpdatePropertyResponse>> updateProperty(
            @AuthenticatedMemberId final long memberId,
            @Positive @PathVariable final long propertyId,
            @Valid @RequestBody final UpdatePropertyRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(UpdatePropertyResponse.from(
                propertyCommandService.updateProperty(memberId, propertyId, request.toCommand())
        )));
    }

    @Operation(summary = "매물 삭제", description = "인증 회원이 소유한 매물을 물리 삭제한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "매물 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "매물이 없거나 다른 회원이 소유함",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "연결 사진 삭제 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/{propertyId}")
    public ResponseEntity<Void> deleteProperty(
            @AuthenticatedMemberId final long memberId,
            @Positive @PathVariable final long propertyId
    ) {
        propertyDeletionService.deleteProperty(memberId, propertyId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "방문 전 사전 메모 저장",
            description = "v1.1 구조화 메모 전체 또는 v1.0 content만 저장하며 마지막으로 반영된 요청을 최종값으로 사용한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "매물 메모 저장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "메모 요청 검증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "매물이 없거나 다른 회원이 소유함",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PutMapping("/{propertyId}/memo")
    public ResponseEntity<ApiResponse<PropertyMemoResponse>> saveMemo(
            @AuthenticatedMemberId final long memberId,
            @Positive @PathVariable final long propertyId,
            @Valid @RequestBody final SavePropertyMemoRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(PropertyMemoResponse.from(
                propertyCommandService.saveMemo(memberId, propertyId, request.toCommand())
        )));
    }
}
