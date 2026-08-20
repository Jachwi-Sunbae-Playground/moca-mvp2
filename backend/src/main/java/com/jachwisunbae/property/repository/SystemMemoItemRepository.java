package com.jachwisunbae.property.repository;

import com.jachwisunbae.property.entity.SystemMemoItem;
import java.util.List;

public interface SystemMemoItemRepository {
    List<SystemMemoItem> findActive();

    List<SystemMemoItem> findActiveByIds(List<Long> ids);

    List<SystemMemoItem> findByIds(List<Long> ids);
}
