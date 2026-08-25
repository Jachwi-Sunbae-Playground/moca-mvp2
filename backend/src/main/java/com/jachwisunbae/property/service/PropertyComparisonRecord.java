package com.jachwisunbae.property.service;

import com.jachwisunbae.property.controller.dto.response.PropertyChecklistStageResponse;
import com.jachwisunbae.property.entity.Property;
import com.jachwisunbae.property.repository.query.PropertyChecklistApplicationQuery;
import com.jachwisunbae.property.repository.query.PropertyMemoQuery;
import java.util.List;

public record PropertyComparisonRecord(
        Property property,
        List<Photo> photos,
        PropertyMemoQuery memo,
        List<Stage> stages) {

    public record Photo(Long id, byte[] bytes, String contentType, boolean representative) {
        public Photo {
            bytes = bytes.clone();
        }

        @Override
        public byte[] bytes() {
            return bytes.clone();
        }
    }

    public record Stage(PropertyChecklistStageResponse summary,
                        PropertyChecklistApplicationQuery application) {
    }
}
