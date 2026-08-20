package com.jachwisunbae.checklist.repository;

import com.jachwisunbae.checklist.entity.SystemCheckItem;
import com.jachwisunbae.checklist.type.CheckStage;

import java.util.List;

public interface SystemCheckItemRepository {

    List<SystemCheckItem> findActiveByStage(CheckStage stage, String question);

    List<SystemCheckItem> findActiveCoreByStage(CheckStage stage);

    List<SystemCheckItem> findByIdsInOrder(List<Long> ids);
}
