package com.jachwisunbae.property.controller.dto.response;

import com.jachwisunbae.property.repository.query.PropertyListItemQuery;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

public record PropertyListItemResponse(Long id, String name, Long depositAmount, Long monthlyRentAmount,
                                       String discoverySource, String address, String roadAddress,
                                       String jibunAddress, BigDecimal latitude, BigDecimal longitude,
                                       int photoCount, PropertyRepresentativePhoto representativePhoto,
                                       PropertyProgress overallProgress,
                                       List<PropertyChecklistStageResponse> stages,
                                       Instant lastActivityAt) {
    public static PropertyListItemResponse from(final PropertyListItemQuery row) {
        PropertyRepresentativePhoto photo = row.photoId() == null ? null : new PropertyRepresentativePhoto(
                row.photoId(), "/api/properties/" + row.propertyId() + "/photos/" + row.photoId(),
                row.photoContentType());
        return from(row, photo, progressFrom(row), List.of());
    }

    public static PropertyListItemResponse from(final PropertyListItemQuery row,
                                                 final PropertyRepresentativePhoto photo) {
        return from(row, photo, progressFrom(row), List.of());
    }

    public static PropertyListItemResponse from(final PropertyListItemQuery row,
                                                 final PropertyRepresentativePhoto photo,
                                                 final PropertyProgress progress) {
        return from(row, photo, progress, List.of());
    }

    public static PropertyListItemResponse from(final PropertyListItemQuery row,
                                                 final PropertyRepresentativePhoto photo,
                                                 final PropertyProgress progress,
                                                 final List<PropertyChecklistStageResponse> stages) {
        return new PropertyListItemResponse(row.propertyId(), row.propertyName(),
                row.depositAmount(), row.monthlyRentAmount(),
                row.discoverySource(), displayAddress(row.roadAddress(), row.jibunAddress()),
                row.roadAddress(), row.jibunAddress(), row.latitude(), row.longitude(), row.photoCount(),
                photo, progress, List.copyOf(stages), row.lastActivityAt().toInstant(ZoneOffset.UTC));
    }

    private static String displayAddress(final String roadAddress, final String jibunAddress) {
        return roadAddress == null || roadAddress.isBlank() ? jibunAddress : roadAddress;
    }

    private static PropertyProgress progressFrom(final PropertyListItemQuery row) {
        int rate = row.totalCount() == 0 ? 0 : row.completedCount() * 100 / row.totalCount();
        return new PropertyProgress(row.totalCount(), row.completedCount(), row.goodCount(),
                row.cautionCount(), row.unconfirmedCount(), rate);
    }
}
