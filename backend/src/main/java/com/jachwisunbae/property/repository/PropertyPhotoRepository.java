package com.jachwisunbae.property.repository;

import com.jachwisunbae.property.entity.PropertyPhoto;
import java.util.List;

public interface PropertyPhotoRepository {
    List<PropertyPhoto> findByPropertyId(long propertyId);
}
