package com.jachwisunbae.checklist.service;

import com.jachwisunbae.checklist.entity.SystemCheckItem;
import com.jachwisunbae.common.exception.BusinessException;
import com.jachwisunbae.common.exception.DomainErrorCode;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

@Component
public class UserChecklistValidator {

    public void validateItemIds(final List<Long> ids) {
        if (ids == null) {
            throw new BusinessException(DomainErrorCode.CHECKLIST_ITEMS_INVALID,
                    "체크리스트 항목 목록은 null일 수 없습니다.");
        }
        if (ids.size() != new HashSet<>(ids).size()) {
            throw new BusinessException(DomainErrorCode.DUPLICATE_CHECK_ITEM,
                    "같은 시스템 체크 항목을 중복해서 추가할 수 없습니다.");
        }
        if (ids.isEmpty() || ids.size() > 30) {
            throw new BusinessException(DomainErrorCode.CHECKLIST_ITEM_COUNT_OUT_OF_RANGE,
                    "체크리스트 항목은 1개 이상 30개 이하여야 합니다.");
        }
    }

    public void validateItemsExist(final List<Long> requestedIds, final List<SystemCheckItem> items) {
        if (requestedIds.size() != items.size()) {
            throw new BusinessException(DomainErrorCode.INVALID_SYSTEM_CHECK_ITEM,
                    "존재하지 않는 시스템 체크 항목이 포함되어 있습니다.");
        }
    }
}
