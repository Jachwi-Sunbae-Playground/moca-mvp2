package com.jachwisunbae.property.repository;

import com.jachwisunbae.property.entity.PropertyMemo;
import com.jachwisunbae.property.entity.PropertyMemoItem;
import com.jachwisunbae.property.repository.query.PropertyMemoQuery;
import java.util.Optional;

public interface PropertyMemoRepository {
    PropertyMemoQuery findQuery(long propertyId);

    Optional<PropertyMemo> findByPropertyId(long propertyId);

    PropertyMemo save(PropertyMemo memo);

    void update(PropertyMemo memo);

    void deleteByPropertyId(long propertyId);

    int updateItem(long propertyMemoId, long systemMemoItemId, String content);


    void saveItem(PropertyMemoItem item);


}
