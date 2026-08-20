package com.jachwisunbae.checklist.service;

import com.jachwisunbae.property.controller.dto.request.ApplyPropertyChecklistRequest;
import com.jachwisunbae.checklist.entity.UserChecklist;
import com.jachwisunbae.checklist.repository.UserChecklistRepository;
import com.jachwisunbae.checklist.type.CheckStage;
import com.jachwisunbae.common.exception.BusinessException;
import com.jachwisunbae.common.exception.DomainErrorCode;
import com.jachwisunbae.property.repository.PropertyChecklistRepository;
import com.jachwisunbae.property.repository.PropertyRepository;
import com.jachwisunbae.property.repository.query.PropertyChecklistApplicationQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PropertyChecklistService {
    private final PropertyRepository propertyRepository;
    private final UserChecklistRepository userChecklistRepository;
    private final PropertyChecklistRepository propertyChecklistRepository;

    public PropertyChecklistService(final PropertyRepository propertyRepository,
                                     final UserChecklistRepository userChecklistRepository,
                                     final PropertyChecklistRepository propertyChecklistRepository) {
        this.propertyRepository = propertyRepository;
        this.userChecklistRepository = userChecklistRepository;
        this.propertyChecklistRepository = propertyChecklistRepository;
    }

    @Transactional
    public PropertyChecklistApplicationQuery apply(final Long memberId, final Long propertyId,
                                                   final CheckStage stage,
                                                   final ApplyPropertyChecklistRequest request) {
        propertyRepository.findByIdAndMemberId(propertyId, memberId)
                .orElseThrow(() -> new BusinessException(DomainErrorCode.PROPERTY_NOT_FOUND,
                        "매물을 찾을 수 없습니다."));
        UserChecklist checklist = userChecklistRepository.findByIdAndMemberId(request.checklistId(), memberId)
                .orElseThrow(() -> new BusinessException(DomainErrorCode.CHECKLIST_NOT_FOUND,
                        "체크리스트를 찾을 수 없습니다."));
        if (checklist.getStage() != stage) {
            throw new BusinessException(DomainErrorCode.PROPERTY_CHECKLIST_STAGE_MISMATCH,
                    "매물 적용 단계와 체크리스트 단계가 다릅니다.");
        }
        return propertyChecklistRepository.replace(propertyId, checklist.getId(), checklist.getName(), stage,
                userChecklistRepository.findItems(checklist.getId()));
    }
}
