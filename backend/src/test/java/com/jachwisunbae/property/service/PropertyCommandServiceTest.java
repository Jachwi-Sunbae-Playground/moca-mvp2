package com.jachwisunbae.property.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jachwisunbae.property.domain.Property;
import com.jachwisunbae.property.repository.PropertyPreVisitMemoRepository;
import com.jachwisunbae.property.repository.PropertyRepository;
import com.jachwisunbae.property.service.dto.command.CreatePropertyCommand;
import com.jachwisunbae.property.service.dto.result.PropertyResult;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PropertyCommandServiceTest {

    @DisplayName("생성 응답 시각은 DB와 같은 마이크로초 정밀도를 사용한다")
    @Test
    void normalizeCreatedTimeToDatabasePrecision() {
        final PropertyRepository propertyRepository = mock(PropertyRepository.class);
        final PropertyPreVisitMemoRepository propertyPreVisitMemoRepository =
                mock(PropertyPreVisitMemoRepository.class);
        when(propertyRepository.save(any(Property.class)))
                .thenAnswer(invocation -> invocation.<Property>getArgument(0).withId(1L));
        final Clock clock = Clock.fixed(
                Instant.parse("2026-08-10T14:16:52.123456789Z"),
                ZoneOffset.UTC
        );
        final PropertyCommandService service = new PropertyCommandService(
                propertyRepository,
                propertyPreVisitMemoRepository,
                clock
        );

        final PropertyResult result = service.createProperty(
                1L,
                new CreatePropertyCommand("매물", 10_000_000L, 500_000L, "발견 경로")
        );

        assertThat(result.createdAt()).isEqualTo(Instant.parse("2026-08-10T14:16:52.123456Z"));
        assertThat(result.updatedAt()).isEqualTo(result.createdAt());
    }
}
