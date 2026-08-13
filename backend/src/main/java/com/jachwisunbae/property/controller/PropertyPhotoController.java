package com.jachwisunbae.property.controller;

import com.jachwisunbae.common.config.OpenApiConfig;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.exception.server.ExternalServiceException;
import com.jachwisunbae.common.resolver.AuthenticatedMemberId;
import com.jachwisunbae.common.response.ApiResponse;
import com.jachwisunbae.common.response.ErrorResponse;
import com.jachwisunbae.property.controller.dto.request.UploadPhotoRequest;
import com.jachwisunbae.property.controller.dto.response.PropertyPhotoListResponse;
import com.jachwisunbae.property.controller.dto.response.PropertyPhotoResponse;
import com.jachwisunbae.property.service.PropertyPhotoService;
import com.jachwisunbae.property.service.dto.command.UploadPhotoCommand;
import com.jachwisunbae.property.service.dto.result.PhotoContentResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Positive;
import java.io.IOException;
import java.net.URI;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/properties/{propertyId}/photos")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class PropertyPhotoController {

    private final PropertyPhotoService propertyPhotoService;

    public PropertyPhotoController(final PropertyPhotoService propertyPhotoService) {
        this.propertyPhotoService = propertyPhotoService;
    }

    @Operation(summary = "매물 사진 목록 조회", description = "소유한 매물의 사진을 업로드 순으로 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사진 목록 조회 성공"),
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
    @GetMapping
    public ResponseEntity<ApiResponse<PropertyPhotoListResponse>> getPhotos(
            @AuthenticatedMemberId final long memberId,
            @Positive @PathVariable final long propertyId
    ) {
        return ResponseEntity.ok(ApiResponse.success(PropertyPhotoListResponse.from(
                propertyPhotoService.getPhotos(memberId, propertyId)
        )));
    }

    @Operation(
            summary = "매물 사진 등록",
            description = "JPEG·PNG·WebP 사진 한 장을 multipart의 file 파트로 등록한다. 파일은 10 MiB 이하이고 매물당 30장까지 허용한다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = UploadPhotoRequest.class)
                    )
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "사진 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "사진 형식·크기·개수 제한 위반",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "매물이 없거나 다른 회원이 소유함",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "객체 저장 또는 메타데이터 저장 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PropertyPhotoResponse>> uploadPhoto(
            @AuthenticatedMemberId final long memberId,
            @Positive @PathVariable final long propertyId,
            @Parameter(description = "등록할 JPEG·PNG·WebP 파일", required = true)
            @RequestPart("file") final MultipartFile file
    ) {
        final PropertyPhotoResponse response = PropertyPhotoResponse.from(propertyPhotoService.uploadPhoto(
                memberId,
                propertyId,
                toCommand(file)
        ));
        final URI location = URI.create(response.contentUrl());
        return ResponseEntity.created(location).body(ApiResponse.success(response));
    }

    @Operation(summary = "매물 사진 본문 조회", description = "소유권을 확인한 뒤 비공개 객체의 원본 바이트를 스트리밍한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "사진 본문 조회 성공",
                    content = @Content(mediaType = "image/*", schema = @Schema(type = "string", format = "binary"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "매물 또는 사진이 없거나 다른 회원이 소유함",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "사진 저장소 조회 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{photoId}/content")
    public ResponseEntity<Resource> getPhotoContent(
            @AuthenticatedMemberId final long memberId,
            @Positive @PathVariable final long propertyId,
            @Positive @PathVariable final long photoId
    ) {
        final PhotoContentResult result = propertyPhotoService.getPhotoContent(memberId, propertyId, photoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .contentLength(result.sizeBytes())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(new InputStreamResource(result.content()));
    }

    @Operation(summary = "매물 사진 삭제", description = "외부 객체가 삭제되거나 이미 없음을 확인한 뒤 DB 메타데이터를 삭제한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "사진 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "매물 또는 사진이 없거나 다른 회원이 소유함",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "사진 객체 또는 메타데이터 삭제 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/{photoId}")
    public ResponseEntity<Void> deletePhoto(
            @AuthenticatedMemberId final long memberId,
            @Positive @PathVariable final long propertyId,
            @Positive @PathVariable final long photoId
    ) {
        propertyPhotoService.deletePhoto(memberId, propertyId, photoId);
        return ResponseEntity.noContent().build();
    }

    private UploadPhotoCommand toCommand(final MultipartFile file) {
        try {
            return new UploadPhotoCommand(file.getContentType(), file.getBytes());
        } catch (IOException exception) {
            throw new ExternalServiceException(ErrorCode.PHOTO_UPLOAD_FAILED, exception);
        }
    }
}
