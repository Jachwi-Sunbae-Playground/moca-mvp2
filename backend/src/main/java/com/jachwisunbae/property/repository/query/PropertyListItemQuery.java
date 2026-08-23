package com.jachwisunbae.property.repository.query;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PropertyListItemQuery(Long propertyId, String propertyName, Long depositAmount,
                                    Long monthlyRentAmount, String discoverySource,
                                    String roadAddress, String jibunAddress,
                                    BigDecimal latitude, BigDecimal longitude,
                                    Long photoId, String photoUrl, String photoContentType,
                                    int photoCount, LocalDateTime lastActivityAt,
                                    int totalCount, int completedCount, int goodCount,
                                    int cautionCount, int unconfirmedCount) {
}
