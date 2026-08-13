package com.jachwisunbae.property.controller.dto.response;

import com.jachwisunbae.property.service.dto.result.ActiveChecklistResult;
import com.jachwisunbae.property.service.dto.result.PropertyDetailResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

public record PropertyDetailResponse(
        long propertyId,
        String name,
        long depositAmount,
        long monthlyRentAmount,
        DiscoverySourceResponse discoverySource,
        PropertyMemoResponse memo,
        List<PropertyActiveChecklistResponse> activeChecklists,
        @Schema(nullable = true) PropertySummaryResponse.RecentVisitResponse recentVisit,
        PhotoPreviewResponse photoPreview,
        DeletionImpactResponse deletionImpact,
        Instant createdAt,
        Instant updatedAt,
        Instant lastActivityAt
) {

    public static PropertyDetailResponse from(final PropertyDetailResult result) {
        return new PropertyDetailResponse(
                result.propertyId(),
                result.name(),
                result.depositAmount(),
                result.monthlyRentAmount(),
                DiscoverySourceResponse.from(result.discoverySource()),
                PropertyMemoResponse.from(result.memo()),
                result.activeChecklists().stream()
                        .map(PropertyActiveChecklistResponse::from)
                        .toList(),
                PropertySummaryResponse.RecentVisitResponse.from(result.recentVisit()),
                new PhotoPreviewResponse(
                        result.photoCount(),
                        result.photoPreview().stream()
                                .map(photo -> new PhotoResponse(
                                        photo.photoId(),
                                        "/api/properties/%d/photos/%d/content".formatted(
                                                result.propertyId(),
                                                photo.photoId()
                                        ),
                                        photo.createdAt()
                                ))
                                .toList()
                ),
                new DeletionImpactResponse(
                        result.visitCount(),
                        result.photoCount(),
                        result.activeChecklists().size()
                ),
                result.createdAt(),
                result.updatedAt(),
                result.lastActivityAt()
        );
    }

    public record PropertyActiveChecklistResponse(String stage, long checklistId, String name, int itemCount) {

        static PropertyActiveChecklistResponse from(final ActiveChecklistResult result) {
            return new PropertyActiveChecklistResponse(
                    result.stage().name(),
                    result.checklistId(),
                    result.name(),
                    result.itemCount()
            );
        }
    }

    public record PhotoPreviewResponse(int totalCount, List<PhotoResponse> photos) {
    }

    public record PhotoResponse(long photoId, String contentUrl, Instant createdAt) {
    }

    public record DeletionImpactResponse(int visitCount, int photoCount, int activeChecklistCount) {
    }
}
