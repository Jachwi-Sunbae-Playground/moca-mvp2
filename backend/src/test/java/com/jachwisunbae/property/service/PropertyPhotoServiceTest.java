package com.jachwisunbae.property.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jachwisunbae.property.entity.Property;
import com.jachwisunbae.property.repository.PropertyPhotoRepository;
import com.jachwisunbae.property.repository.PropertyRepository;
import com.jachwisunbae.property.storage.PhotoStorage;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class PropertyPhotoServiceTest {

    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M/wHwAF/gL+Xw2YAAAAAElFTkSuQmCC");

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private PropertyPhotoRepository propertyPhotoRepository;

    @Mock
    private PhotoStorage photoStorage;

    private PropertyPhotoService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-24T00:00:00Z"), ZoneOffset.UTC);
        service = new PropertyPhotoService(propertyRepository, propertyPhotoRepository, photoStorage, clock, "test");
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        when(propertyRepository.findByIdAndMemberIdForUpdate(10L, 1L)).thenReturn(Optional.of(
                Property.reconstruct(10L, 1L, "테스트 매물", 0L, 0L, "", null, null,
                        new BigDecimal("37.5"), new BigDecimal("127.0"), now, now, now)));
        when(propertyPhotoRepository.countByPropertyId(10L)).thenReturn(0);
    }

    @Test
    void 객체_업로드_뒤_DB_저장이_실패하면_업로드한_객체를_보상_삭제한다() {
        doThrow(new IllegalStateException("database unavailable"))
                .when(propertyPhotoRepository).save(anyLong(), any(), anyString());
        MockMultipartFile file = new MockMultipartFile("file", "room.png", "image/png", PNG);

        assertThatThrownBy(() -> service.upload(1L, 10L, file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");

        verify(photoStorage).upload(anyString(), any(byte[].class), anyString());
        verify(photoStorage).delete(anyString());
    }
}
