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
import com.jachwisunbae.property.repository.PropertyMemoRepository;
import com.jachwisunbae.property.repository.PropertyChecklistRepository;
import com.jachwisunbae.property.repository.PropertyProgressRepository;
import com.jachwisunbae.member.repository.MemberRepository;
import com.jachwisunbae.common.exception.BusinessException;
import com.jachwisunbae.common.exception.DomainErrorCode;
import com.jachwisunbae.property.repository.query.PropertyListItemQuery;
import com.jachwisunbae.property.repository.query.PropertyProgressSummary;
import com.jachwisunbae.property.repository.query.PropertyPhotosQuery;
import com.jachwisunbae.property.repository.query.PropertyChecklistProgressQuery;
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
    private final PropertyMemoRepository propertyMemoRepository;
    private final PropertyChecklistRepository propertyChecklistRepository;
    private final PropertyProgressRepository propertyProgressRepository;

    public PropertyService(final PropertyRepository propertyRepository, final MemberRepository memberRepository,
                           final PropertyPhotoRepository propertyPhotoRepository,
                           final PropertyProgressRepository propertyProgressRepository,
                           final PropertyMemoRepository propertyMemoRepository,
                           final PropertyChecklistRepository propertyChecklistRepository) {
        this.propertyRepository = propertyRepository;
        this.memberRepository = memberRepository;
        this.propertyPhotoRepository = propertyPhotoRepository;
        this.propertyProgressRepository = propertyProgressRepository;
        this.propertyMemoRepository = propertyMemoRepository;
        this.propertyChecklistRepository = propertyChecklistRepository;
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

    @Transactional
    public void delete(final Long memberId, final Long propertyId) {
        propertyRepository.findByIdAndMemberId(propertyId, memberId)
                .orElseThrow(() -> new BusinessException(DomainErrorCode.PROPERTY_NOT_FOUND,
                        "매물을 찾을 수 없습니다."));
        propertyMemoRepository.deleteByPropertyId(propertyId);
        propertyChecklistRepository.deleteByPropertyId(propertyId);
        propertyPhotoRepository.deleteByPropertyId(propertyId);
        propertyRepository.deleteById(propertyId);
    }

    public PropertyPhotosQuery findPhotos(final Long memberId, final Long propertyId) {
        propertyRepository.findByIdAndMemberId(propertyId, memberId)
            .orElseThrow(() -> new BusinessException(DomainErrorCode.PROPERTY_NOT_FOUND,
                "매물을 찾을 수 없습니다."));
        return new PropertyPhotosQuery(propertyId, propertyPhotoRepository.findByPropertyId(propertyId),
            propertyPhotoRepository.findRepresentativePhotoId(propertyId).orElse(null));
    }

    public List<PropertyChecklistProgressQuery> findChecklistOverview(final Long memberId, final Long propertyId) {
        propertyRepository.findByIdAndMemberId(propertyId, memberId)
                .orElseThrow(() -> new BusinessException(DomainErrorCode.PROPERTY_NOT_FOUND,
                        "매물을 찾을 수 없습니다."));
        return propertyProgressRepository.findByPropertyIdAndStage(propertyId);
    }

    @Transactional
    public void deletePhoto(final Long memberId, final Long propertyId, final Long photoId) {
        propertyRepository.findByIdAndMemberId(propertyId, memberId)
            .orElseThrow(() -> new BusinessException(DomainErrorCode.PROPERTY_NOT_FOUND,
                "매물을 찾을 수 없습니다."));
        propertyPhotoRepository.findByIdAndPropertyId(photoId, propertyId)
            .orElseThrow(() -> new BusinessException(DomainErrorCode.PHOTO_NOT_FOUND,
                "사진을 찾을 수 없습니다."));
        propertyPhotoRepository.deleteById(photoId);
        propertyPhotoRepository.ensureRepresentative(propertyId);
    }

    @Transactional
    public void designateRepresentativePhoto(final Long memberId, final Long propertyId, final Long photoId) {
        propertyRepository.findByIdAndMemberId(propertyId, memberId)
            .orElseThrow(() -> new BusinessException(DomainErrorCode.PROPERTY_NOT_FOUND,
                "매물을 찾을 수 없습니다."));
        propertyPhotoRepository.findByIdAndPropertyId(photoId, propertyId)
            .orElseThrow(() -> new BusinessException(DomainErrorCode.PHOTO_NOT_FOUND,
                "사진을 찾을 수 없습니다."));
        propertyPhotoRepository.setRepresentative(propertyId, photoId);
    }
}
