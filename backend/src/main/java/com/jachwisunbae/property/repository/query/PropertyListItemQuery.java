package com.jachwisunbae.property.repository.query;

public record PropertyListItemQuery(Long propertyId, String propertyName, Long depositAmount,
                                    Long monthlyRentAmount, String discoverySource,
                                    Long photoId, String photoUrl, String photoContentType,
                                    int totalCount, int completedCount, int goodCount,
                                    int cautionCount, int unconfirmedCount) {
}
