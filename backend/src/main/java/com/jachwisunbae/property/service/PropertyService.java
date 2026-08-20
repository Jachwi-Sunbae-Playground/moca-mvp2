package com.jachwisunbae.property.service;

import com.jachwisunbae.property.controller.dto.request.UpdatePropertyRequest;
import com.jachwisunbae.property.controller.dto.response.PropertyListResponse;
import com.jachwisunbae.property.controller.dto.response.PropertyDetailResponse;
import com.jachwisunbae.property.controller.dto.response.PropertyProgress;
import com.jachwisunbae.property.controller.dto.response.PropertyListItemResponse;
import com.jachwisunbae.property.controller.dto.request.CreatePropertyRequest;
import com.jachwisunbae.property.entity.Property;
import com.jachwisunbae.property.repository.PropertyRepository;
import com.jachwisunbae.property.repository.PropertyPhotoRepository;
import com.jachwisunbae.property.repository.PropertyProgressRepository;
import com.jachwisunbae.member.repository.MemberRepository;
import com.jachwisunbae.common.exception.BusinessException;
import com.jachwisunbae.common.exception.DomainErrorCode;
import com.jachwisunbae.property.repository.query.PropertyListItemQuery;
import com.jachwisunbae.property.repository.query.PropertyProgressSummary;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PropertyService {
    private final PropertyRepository propertyRepository;
    private final MemberRepository memberRepository;
    private final PropertyPhotoRepository propertyPhotoRepository;
    private final PropertyProgressRepository propertyProgressRepository;

    public PropertyService(final PropertyRepository propertyRepository, final MemberRepository memberRepository,
                           final PropertyPhotoRepository propertyPhotoRepository,
                           final PropertyProgressRepository propertyProgressRepository) {
        this.propertyRepository = propertyRepository;
        this.memberRepository = memberRepository;
        this.propertyPhotoRepository = propertyPhotoRepository;
        this.propertyProgressRepository = propertyProgressRepository;
    }

    public PropertyListResponse findList(final Long memberId) {
        List<PropertyListItemResponse> items = propertyRepository.findListByMemberId(memberId).stream()
                .map(PropertyListItemResponse::from)
                .toList();
        return new PropertyListResponse(items.size(), items);
    }

    public PropertyDetailResponse findDetail(final Long memberId, final Long propertyId) {
        Property property = propertyRepository.findByIdAndMemberId(propertyId, memberId)
                .orElseThrow(() -> new BusinessException(DomainErrorCode.PROPERTY_NOT_FOUND,
                        "매물을 찾을 수 없습니다."));
        return PropertyDetailResponse.from(property, propertyPhotoRepository.findByPropertyId(propertyId),
                PropertyProgress.from(propertyProgressRepository.findByPropertyId(propertyId)));
    }

    @Transactional
    public Property create(final Long memberId, final CreatePropertyRequest request) {
        memberRepository.findByIdForUpdate(memberId).orElseThrow(() -> new BusinessException(
                DomainErrorCode.MEMBER_NOT_FOUND, "회원을 찾을 수 없습니다."));
        validatePropertyCount(memberId);

        return propertyRepository.save(Property.create(memberId, request.name(), request.depositAmount(),
                request.monthlyRentAmount(), request.discoverySource()));
    }

    private void validatePropertyCount(final Long memberId) {
        if (propertyRepository.countByMemberId(memberId) >= 30) {
            throw new BusinessException(DomainErrorCode.PROPERTY_LIMIT_EXCEEDED,
                    "회원당 매물은 30개까지 등록할 수 있습니다.");
        }
    }

    @Transactional
    public Property update(final Long memberId, final Long propertyId, final UpdatePropertyRequest request) {
        Property property = propertyRepository.findByIdAndMemberId(propertyId, memberId)
                .orElseThrow(() -> new BusinessException(DomainErrorCode.PROPERTY_NOT_FOUND,
                        "매물을 찾을 수 없습니다."));
        property.replaceBasicInfo(request.name(), request.depositAmount(),
                request.monthlyRentAmount(), request.discoverySource());
        return propertyRepository.update(property);
    }

}
