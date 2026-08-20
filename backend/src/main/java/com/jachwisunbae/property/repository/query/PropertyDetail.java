package com.jachwisunbae.property.repository.query;

public record PropertyDetail(
    Long id,
    String name,
    Long depositAmount,
    Long monthlyRentAmount,
    String discoverySource,
    String representativePhotoUrl
) {
}
