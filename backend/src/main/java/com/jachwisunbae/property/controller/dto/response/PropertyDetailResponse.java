package com.jachwisunbae.property.controller.dto.response;

import com.jachwisunbae.property.entity.Property;
import com.jachwisunbae.property.entity.PropertyPhoto;
import java.util.List;

public record PropertyDetailResponse(Long id, String name, Long depositAmount, Long monthlyRentAmount,
                                     String discoverySource, List<PropertyDetailPhoto> photos, PropertyProgress overallProgress) {
    public static PropertyDetailResponse from(final Property property, final List<PropertyPhoto> photos,
                                               final PropertyProgress progress) {
        return new PropertyDetailResponse(property.getId(), property.getName(), property.getDepositAmount(),
                property.getMonthlyRentAmount(), property.getDiscoverySource(),
                photos.stream().map(PropertyDetailPhoto::from).toList(),
                progress);
    }
}
