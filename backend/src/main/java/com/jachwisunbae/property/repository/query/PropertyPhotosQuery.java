package com.jachwisunbae.property.repository.query;

import com.jachwisunbae.property.entity.PropertyPhoto;
import java.util.List;

public record PropertyPhotosQuery(Long propertyId, List<PropertyPhoto> photos, Long representativePhotoId) {
}
