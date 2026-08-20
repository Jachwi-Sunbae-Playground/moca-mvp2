package com.jachwisunbae.property.controller.dto.response;

import com.jachwisunbae.property.repository.query.PropertyListItemQuery;

public record PropertyListItemResponse(Long id, String name, Long depositAmount, Long monthlyRentAmount,
                                       String discoverySource, PropertyRepresentativePhoto representativePhoto,
                                       PropertyProgress overallProgress) {
    public static PropertyListItemResponse from(final PropertyListItemQuery row) {
        return from(row, null, progressFrom(row));
    }

    public static PropertyListItemResponse from(final PropertyListItemQuery row,
                                                 final PropertyRepresentativePhoto photo) {
        return from(row, photo, progressFrom(row));
    }

    public static PropertyListItemResponse from(final PropertyListItemQuery row,
                                                 final PropertyRepresentativePhoto photo,
                                                 final PropertyProgress progress) {
        return new PropertyListItemResponse(row.propertyId(), row.propertyName(),
                row.depositAmount(), row.monthlyRentAmount(),
                row.discoverySource(),
                photo,
                progress);
    }

    private static PropertyProgress progressFrom(final PropertyListItemQuery row) {
        int rate = row.totalCount() == 0 ? 0 : row.completedCount() * 100 / row.totalCount();
        return new PropertyProgress(row.totalCount(), row.completedCount(), row.goodCount(),
                row.cautionCount(), row.unconfirmedCount(), rate);
    }
}
