package com.jachwisunbae.property.service;

import com.jachwisunbae.property.entity.SystemMemoItem;
import com.jachwisunbae.property.repository.SystemMemoItemRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SystemMemoItemService {
    private final SystemMemoItemRepository systemMemoItemRepository;

    public SystemMemoItemService(final SystemMemoItemRepository systemMemoItemRepository) {
        this.systemMemoItemRepository = systemMemoItemRepository;
    }

    public List<SystemMemoItem> findActive() {
        return systemMemoItemRepository.findActive();
    }
}
