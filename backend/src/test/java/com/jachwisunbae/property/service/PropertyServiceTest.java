package com.jachwisunbae.property.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jachwisunbae.member.entity.Member;
import com.jachwisunbae.member.repository.MemberRepository;
import com.jachwisunbae.property.controller.dto.request.CreatePropertyRequest;
import com.jachwisunbae.property.entity.Property;
import com.jachwisunbae.property.repository.PropertyPhotoRepository;
import com.jachwisunbae.property.repository.PropertyProgressRepository;
import com.jachwisunbae.property.repository.PropertyRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PropertyServiceTest {

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PropertyPhotoRepository propertyPhotoRepository;

    @Mock
    private PropertyProgressRepository propertyProgressRepository;

    @Mock
    private PropertyMemoInitializer propertyMemoInitializer;

    @InjectMocks
    private PropertyService propertyService;

    @Test
    void 매물을_생성하면_해당_매물의_메모를_초기화한다() {
        CreatePropertyRequest request = new CreatePropertyRequest("역세권 원룸", 10_000L, 500L, "부동산");
        Property saved = Property.reconstruct(10L, 1L, "역세권 원룸", 10_000L, 500L, "부동산");
        when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mock(Member.class)));
        when(propertyRepository.countByMemberId(1L)).thenReturn(0);
        when(propertyRepository.save(any(Property.class))).thenReturn(saved);

        Property result = propertyService.create(1L, request);

        assertThat(result).isSameAs(saved);
        verify(propertyMemoInitializer).initialize(10L);
    }
}
