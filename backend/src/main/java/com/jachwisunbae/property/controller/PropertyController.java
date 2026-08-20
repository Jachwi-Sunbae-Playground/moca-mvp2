package com.jachwisunbae.property.controller;

import com.jachwisunbae.auth.web.AuthenticatedMemberId;
import com.jachwisunbae.common.web.ApiResponse;
import com.jachwisunbae.property.controller.dto.request.CreatePropertyRequest;
import com.jachwisunbae.property.controller.dto.request.UpdatePropertyRequest;
import com.jachwisunbae.property.controller.dto.request.ApplyPropertyChecklistRequest;
import com.jachwisunbae.property.controller.dto.request.UpdatePropertyChecklistMemoRequest;
import com.jachwisunbae.property.controller.dto.request.UpdatePropertyChecklistStatusRequest;
import com.jachwisunbae.property.service.PropertyChecklistService;
import com.jachwisunbae.checklist.type.CheckStage;
import com.jachwisunbae.property.controller.dto.response.PropertyChecklistApplicationResponse;
import com.jachwisunbae.property.controller.dto.request.UpdatePropertyMemoRequest;
import com.jachwisunbae.property.controller.dto.response.CreatePropertyResponse;
import com.jachwisunbae.property.controller.dto.response.PropertyDetailResponse;
import com.jachwisunbae.property.controller.dto.response.PropertyListResponse;
import com.jachwisunbae.property.controller.dto.response.UpdatePropertyResponse;
import com.jachwisunbae.property.controller.dto.response.PropertyMemoResponse;
import com.jachwisunbae.property.controller.dto.response.PropertyPhotoListResponse;
import com.jachwisunbae.property.controller.dto.response.PropertyPhotoResponse;
import com.jachwisunbae.property.controller.dto.response.PropertyChecklistOverviewResponse;
import com.jachwisunbae.property.controller.dto.response.PropertyChecklistItemMemoResponse;
import com.jachwisunbae.property.controller.dto.response.PropertyChecklistItemStatusResponse;
import com.jachwisunbae.property.entity.Property;
import com.jachwisunbae.property.repository.query.PropertyPhotosQuery;
import com.jachwisunbae.property.service.PropertyService;
import com.jachwisunbae.property.service.PropertyMemoService;
import com.jachwisunbae.property.service.PropertyPhotoService;
import com.jachwisunbae.property.service.PropertyDeletionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
    private final PropertyChecklistService propertyChecklistService;
    private final PropertyPhotoService propertyPhotoService;
    private final PropertyDeletionService propertyDeletionService;

    public PropertyController(final PropertyService propertyService,
                              final PropertyMemoService propertyMemoService,
                              final PropertyChecklistService propertyChecklistService,
                              final PropertyPhotoService propertyPhotoService,
                              final PropertyDeletionService propertyDeletionService) {
        this.propertyService = propertyService;
        this.propertyMemoService = propertyMemoService;
        this.propertyChecklistService = propertyChecklistService;
        this.propertyPhotoService = propertyPhotoService;
        this.propertyDeletionService = propertyDeletionService;
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
        propertyDeletionService.delete(memberId, propertyId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{propertyId}/photos")
    public ApiResponse<PropertyPhotoListResponse> findPhotos(
            @AuthenticatedMemberId final Long memberId,
            @PathVariable final Long propertyId) {
        PropertyPhotosQuery query = propertyPhotoService.find(memberId, propertyId);
        List<PropertyPhotoResponse> items =  query.photos().stream()
                .map(photo -> PropertyPhotoResponse.from(photo, photo.getId().equals(query.representativePhotoId())))
                .toList();
        return ApiResponse.of("사진 목록을 조회했습니다.",
                new PropertyPhotoListResponse(query.propertyId(), items.size(), items));
    }

    @DeleteMapping("/{propertyId}/photos/{photoId}")
    public ResponseEntity<Void> deletePhoto(
            @AuthenticatedMemberId final Long memberId,
            @PathVariable final Long propertyId,
            @PathVariable final Long photoId) {
        propertyPhotoService.delete(memberId, propertyId, photoId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{propertyId}/photos/{photoId}/representative")
    public ResponseEntity<Void> designateRepresentativePhoto(
            @AuthenticatedMemberId final Long memberId,
            @PathVariable final Long propertyId,
            @PathVariable final Long photoId) {
        propertyPhotoService.designateRepresentative(memberId, propertyId, photoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{propertyId}/checklists")
    public ApiResponse<PropertyChecklistOverviewResponse> findChecklistOverview(
            @AuthenticatedMemberId final Long memberId,
            @PathVariable final Long propertyId) {
        return ApiResponse.of("매물 체크 현황을 조회했습니다.",
                PropertyChecklistOverviewResponse.from(propertyId,
                        propertyChecklistService.findOverview(memberId, propertyId)));
    }

    @PutMapping("/{propertyId}/checklists/{stage}")
    public ApiResponse<PropertyChecklistApplicationResponse> applyChecklist(
            @AuthenticatedMemberId final Long memberId,
            @PathVariable final Long propertyId,
            @PathVariable final CheckStage stage,
            @Valid @RequestBody final ApplyPropertyChecklistRequest request) {
        return ApiResponse.of("매물 단계 체크리스트를 적용했습니다.",
                PropertyChecklistApplicationResponse.from(
                        propertyChecklistService.apply(memberId, propertyId, stage, request)));
    }

    @PatchMapping("/{propertyId}/checklists/{propertyChecklistId}/items/{itemId}/status")
    @Operation(summary = "매물 체크 항목 상태 저장", description = "상태 컬럼만 갱신합니다.")
    public ApiResponse<PropertyChecklistItemStatusResponse> updateChecklistItemStatus(
            @AuthenticatedMemberId final Long memberId,
            @PathVariable final Long propertyId,
            @PathVariable final Long propertyChecklistId,
            @PathVariable final Long itemId,
            @Valid @RequestBody final UpdatePropertyChecklistStatusRequest request) {
        var item = propertyChecklistService.updateStatus(memberId, propertyId, propertyChecklistId, itemId, request);
        return ApiResponse.of("체크 상태를 저장했습니다.",
                new PropertyChecklistItemStatusResponse(
                        new PropertyChecklistItemStatusItem(item.id(), item.status())));
    }

    @PatchMapping("/{propertyId}/checklists/{propertyChecklistId}/items/{itemId}/memo")
    @Operation(summary = "매물 체크 항목 메모 저장", description = "메모 컬럼만 갱신합니다.")
    public ApiResponse<PropertyChecklistItemMemoResponse> updateChecklistItemMemo(
            @AuthenticatedMemberId final Long memberId,
            @PathVariable final Long propertyId,
            @PathVariable final Long propertyChecklistId,
            @PathVariable final Long itemId,
            @Valid @RequestBody final UpdatePropertyChecklistMemoRequest request) {
        var item = propertyChecklistService.updateMemo(memberId, propertyId, propertyChecklistId, itemId, request);
        return ApiResponse.of("항목 메모를 저장했습니다.",
                new PropertyChecklistItemMemoResponse(new PropertyChecklistItemMemoItem(item.id(), item.memo())));
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
