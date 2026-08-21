package com.jachwisunbae.property.service;

import com.jachwisunbae.property.entity.PropertyMemo;
import com.jachwisunbae.property.entity.PropertyMemoItem;
import com.jachwisunbae.property.repository.PropertyMemoRepository;
import com.jachwisunbae.property.repository.SystemMemoItemRepository;
import org.springframework.stereotype.Component;

@Component
public class PropertyMemoInitializer {

    private final PropertyMemoRepository propertyMemoRepository;
    private final SystemMemoItemRepository systemMemoItemRepository;

    public PropertyMemoInitializer(final PropertyMemoRepository propertyMemoRepository,
                                   final SystemMemoItemRepository systemMemoItemRepository) {
        this.propertyMemoRepository = propertyMemoRepository;
        this.systemMemoItemRepository = systemMemoItemRepository;
    }

    public void initialize(final long propertyId) {
        if (propertyMemoRepository.findByPropertyId(propertyId).isPresent()) {
            return;
        }

        PropertyMemo memo = propertyMemoRepository.save(PropertyMemo.create(propertyId, ""));
        systemMemoItemRepository.findActive().forEach(item -> propertyMemoRepository.saveItem(
                PropertyMemoItem.create(memo.getId(), item.getId(), item.getLabel(), item.getDisplayOrder(), "")));
    }
}
