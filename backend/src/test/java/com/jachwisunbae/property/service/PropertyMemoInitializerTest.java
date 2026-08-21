package com.jachwisunbae.property.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jachwisunbae.property.entity.PropertyMemo;
import com.jachwisunbae.property.entity.PropertyMemoItem;
import com.jachwisunbae.property.entity.SystemMemoItem;
import com.jachwisunbae.property.repository.PropertyMemoRepository;
import com.jachwisunbae.property.repository.SystemMemoItemRepository;
import java.util.List;
import java.util.Optional;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PropertyMemoInitializerTest {

    @Mock
    private PropertyMemoRepository propertyMemoRepository;

    @Mock
    private SystemMemoItemRepository systemMemoItemRepository;

    @InjectMocks
    private PropertyMemoInitializer propertyMemoInitializer;

    @Test
    void 메모가_없으면_빈_메모와_활성_시스템_메모_항목을_생성한다() {
        SystemMemoItem first = SystemMemoItem.reconstruct(11L, "교통", 1, null);
        SystemMemoItem second = SystemMemoItem.reconstruct(12L, "주변 시설", 2, null);
        when(propertyMemoRepository.findByPropertyId(1L)).thenReturn(Optional.empty());
        when(propertyMemoRepository.save(any(PropertyMemo.class)))
                .thenReturn(PropertyMemo.reconstruct(100L, 1L, ""));
        when(systemMemoItemRepository.findActive()).thenReturn(List.of(first, second));

        propertyMemoInitializer.initialize(1L);

        ArgumentCaptor<PropertyMemo> memoCaptor = ArgumentCaptor.forClass(PropertyMemo.class);
        verify(propertyMemoRepository).save(memoCaptor.capture());
        assertThat(memoCaptor.getValue().getPropertyId()).isEqualTo(1L);
        assertThat(memoCaptor.getValue().getFreeMemo()).isEmpty();

        ArgumentCaptor<PropertyMemoItem> captor = ArgumentCaptor.forClass(PropertyMemoItem.class);
        verify(propertyMemoRepository, times(2)).saveItem(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(PropertyMemoItem::getPropertyMemoId,
                        PropertyMemoItem::getSystemMemoItemId,
                        PropertyMemoItem::getLabel,
                        PropertyMemoItem::getDisplayOrder,
                        PropertyMemoItem::getContent)
                .containsExactly(
                        Tuple.tuple(100L, 11L, "교통", 1, ""),
                        Tuple.tuple(100L, 12L, "주변 시설", 2, ""));
    }

    @Test
    void 메모가_이미_있으면_다시_생성하지_않는다() {
        when(propertyMemoRepository.findByPropertyId(1L))
                .thenReturn(Optional.of(PropertyMemo.reconstruct(100L, 1L, "")));

        propertyMemoInitializer.initialize(1L);

        verify(propertyMemoRepository, never()).save(any());
        verify(propertyMemoRepository, never()).saveItem(any());
        verify(systemMemoItemRepository, never()).findActive();
    }
}
