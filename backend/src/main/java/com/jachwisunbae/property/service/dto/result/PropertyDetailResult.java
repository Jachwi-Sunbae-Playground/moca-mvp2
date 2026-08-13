package com.jachwisunbae.property.service.dto.result;

import com.jachwisunbae.property.domain.DiscoverySource;
import com.jachwisunbae.property.repository.PropertyDetailProjection;
import java.time.Instant;
import java.util.List;

public record PropertyDetailResult(
        long propertyId,
        String name,
        long depositAmount,
        long monthlyRentAmount,
        DiscoverySource discoverySource,
        PropertyMemoResult memo,
        int photoCount,
        RecentVisitResult recentVisit,
        int visitCount,
        List<ActiveChecklistResult> activeChecklists,
        List<PropertyPhotoResult> photoPreview,
        Instant createdAt,
        Instant updatedAt,
        Instant lastActivityAt
) {

    public PropertyDetailResult {
        activeChecklists = List.copyOf(activeChecklists);
        photoPreview = List.copyOf(photoPreview);
    }

    public static PropertyDetailResult from(
            final PropertyDetailProjection projection,
            final List<ActiveChecklistResult> activeChecklists,
            final List<PropertyPhotoResult> photoPreview
    ) {
        return new PropertyDetailResult(
                projection.propertyId(),
                projection.name().value(),
                projection.depositAmount().amount(),
                projection.monthlyRentAmount().amount(),
                projection.discoverySource(),
                PropertyMemoResult.from(projection.memo()),
                projection.photoCount(),
                RecentVisitResult.from(projection.recentVisit()),
                projection.visitCount(),
                activeChecklists,
                photoPreview,
                projection.createdAt(),
                projection.updatedAt(),
                projection.lastActivityAt()
        );
    }
}
