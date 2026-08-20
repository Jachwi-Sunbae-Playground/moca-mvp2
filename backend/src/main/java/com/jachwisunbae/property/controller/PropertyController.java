package com.jachwisunbae.property.controller;

import com.jachwisunbae.auth.web.AuthenticatedMemberId;
import com.jachwisunbae.common.web.ApiResponse;
import com.jachwisunbae.property.controller.dto.request.CreatePropertyRequest;
import com.jachwisunbae.property.controller.dto.request.UpdatePropertyRequest;
import com.jachwisunbae.property.controller.dto.request.UpdatePropertyMemoRequest;
import com.jachwisunbae.property.controller.dto.response.CreatePropertyResponse;
import com.jachwisunbae.property.controller.dto.response.PropertyDetailResponse;
import com.jachwisunbae.property.controller.dto.response.PropertyListResponse;
import com.jachwisunbae.property.controller.dto.response.UpdatePropertyResponse;
import com.jachwisunbae.property.controller.dto.response.PropertyMemoResponse;
import com.jachwisunbae.property.entity.Property;
import com.jachwisunbae.property.service.PropertyService;
import com.jachwisunbae.property.service.PropertyMemoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/properties")
@Tag(name = "Properties", description = "후보 매물 관리 API")
public class PropertyController {
    private final PropertyService propertyService;
    private final PropertyMemoService propertyMemoService;

    public PropertyController(final PropertyService propertyService,
                              final PropertyMemoService propertyMemoService) {
        this.propertyService = propertyService;
        this.propertyMemoService = propertyMemoService;
    }

    @GetMapping
    public ApiResponse<PropertyListResponse> findList(@AuthenticatedMemberId final Long memberId) {
        return ApiResponse.of("매물 목록을 조회했습니다.", propertyService.findList(memberId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CreatePropertyResponse>> create(
            @AuthenticatedMemberId final Long memberId,
            @Valid @RequestBody final CreatePropertyRequest request) {
        Property property = propertyService.create(memberId, request);
        return ResponseEntity.created(URI.create("/api/properties/" + property.getId()))
                .body(ApiResponse.of("매물을 등록했습니다.", CreatePropertyResponse.from(property)));
    }

    @GetMapping("{propertyId}")
    public ApiResponse<PropertyDetailResponse> findDetail(@AuthenticatedMemberId final Long memberId, @PathVariable final Long propertyId) {
        return ApiResponse.of("매물 상세 정보를 조회했습니다.",
                propertyService.findDetail(memberId, propertyId));
    }

    @PutMapping("{propertyId}")
    public ApiResponse<UpdatePropertyResponse> update(
        @AuthenticatedMemberId final Long memberId,
        @PathVariable final Long propertyId,
        @Valid @RequestBody final UpdatePropertyRequest request) {
        UpdatePropertyResponse updatePropertyResponse = UpdatePropertyResponse.from(propertyService.update(memberId, propertyId, request));
        return ApiResponse.of("매물 정보를 수정했습니다.",  updatePropertyResponse);
    }

    @DeleteMapping("/{propertyId}")
    public ResponseEntity<Void> delete(
            @AuthenticatedMemberId final Long memberId,
            @PathVariable final Long propertyId) {
        propertyService.delete(memberId, propertyId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{propertyId}/memo")
    public ApiResponse<PropertyMemoResponse> findMemo(
            @AuthenticatedMemberId final Long memberId,
            @PathVariable final Long propertyId) {
        return ApiResponse.of("매물 메모를 조회했습니다.",
                PropertyMemoResponse.from(propertyMemoService.find(memberId, propertyId)));
    }

    @PostMapping("/{propertyId}/memo")
    public ApiResponse<PropertyMemoResponse> initializeMemo(
            @AuthenticatedMemberId final Long memberId,
            @PathVariable final Long propertyId) {
        return ApiResponse.of("매물 메모를 생성했습니다.",
                PropertyMemoResponse.from(propertyMemoService.initialize(memberId, propertyId)));
    }

    @PutMapping("/{propertyId}/memo")
    public ApiResponse<PropertyMemoResponse> updateMemo(
            @AuthenticatedMemberId final Long memberId,
            @PathVariable final Long propertyId,
            @Valid @RequestBody final UpdatePropertyMemoRequest request) {
        return ApiResponse.of("매물 메모를 저장했습니다.",
                PropertyMemoResponse.from(propertyMemoService.update(memberId, propertyId, request)));
    }

}
