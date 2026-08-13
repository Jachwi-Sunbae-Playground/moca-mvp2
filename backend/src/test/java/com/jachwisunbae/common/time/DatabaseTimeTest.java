package com.jachwisunbae.common.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DatabaseTimeTest {

    @DisplayName("DB에 저장할 시각은 DATETIME(6) 정밀도에 맞춘다")
    @Test
    void normalizeToMicroseconds() {
        final Instant instant = Instant.parse("2026-08-10T14:16:52.123456789Z");

        final Instant normalized = DatabaseTime.normalize(instant);

        assertThat(normalized).isEqualTo(Instant.parse("2026-08-10T14:16:52.123456Z"));
    }
}
