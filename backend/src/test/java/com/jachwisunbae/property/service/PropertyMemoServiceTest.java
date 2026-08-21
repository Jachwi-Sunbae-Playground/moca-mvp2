package com.jachwisunbae.property.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jachwisunbae.property.entity.Property;
import com.jachwisunbae.property.repository.PropertyMemoRepository;
import com.jachwisunbae.property.repository.PropertyRepository;
import com.jachwisunbae.property.repository.query.PropertyMemoQuery;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PropertyMemoServiceTest {

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private PropertyMemoRepository propertyMemoRepository;

    @Mock
    private PropertyMemoInitializer propertyMemoInitializer;

    @InjectMocks
    private PropertyMemoService propertyMemoService;

    @Test
    void 메모_초기화는_소유_매물을_잠근_뒤_멱등하게_처리한다() {
        PropertyMemoQuery expected = new PropertyMemoQuery(10L, "", List.of());
        when(propertyRepository.findByIdAndMemberIdForUpdate(10L, 1L))
                .thenReturn(Optional.of(mock(Property.class)));
        when(propertyMemoRepository.findQuery(10L)).thenReturn(expected);

        PropertyMemoQuery result = propertyMemoService.initialize(1L, 10L);

        assertThat(result).isSameAs(expected);
        InOrder order = inOrder(propertyRepository, propertyMemoInitializer);
        order.verify(propertyRepository).findByIdAndMemberIdForUpdate(10L, 1L);
        order.verify(propertyMemoInitializer).initialize(10L);
    }
}
