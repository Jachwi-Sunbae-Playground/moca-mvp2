package com.jachwisunbae.checklist.service;

import com.jachwisunbae.checklist.entity.SystemCheckItem;
import com.jachwisunbae.checklist.repository.SystemCheckItemRepository;
import com.jachwisunbae.checklist.type.CheckStage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SystemCheckItemService {

    private final SystemCheckItemRepository systemCheckItemRepository;

    public SystemCheckItemService(final SystemCheckItemRepository systemCheckItemRepository) {
        this.systemCheckItemRepository = systemCheckItemRepository;
    }

    public List<SystemCheckItem> search(final CheckStage stage, final String query) {
        return systemCheckItemRepository.findActiveByStage(stage, query);
    }
}
