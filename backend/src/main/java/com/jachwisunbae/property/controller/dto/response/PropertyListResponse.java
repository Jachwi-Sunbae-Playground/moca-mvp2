package com.jachwisunbae.property.controller.dto.response;

import com.jachwisunbae.property.repository.query.PropertyListItemQuery;

import java.util.List;
import java.util.Map;

public record PropertyListResponse(int totalCount, List<PropertyListItemResponse> items) {

    public static PropertyListResponse from(final List<PropertyListItemQuery> rows) {
        return new PropertyListResponse(rows.size(), rows.stream().map(PropertyListItemResponse::from).toList());
    }

    public static PropertyListResponse from(final List<PropertyListItemQuery> rows,
                                            final Map<Long, PropertyRepresentativePhoto> photos) {
        return new PropertyListResponse(rows.size(), rows.stream()
                .map(row -> PropertyListItemResponse.from(row, photos.get(row.propertyId())))
                .toList());
    }

}
