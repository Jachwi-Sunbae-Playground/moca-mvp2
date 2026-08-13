package com.jachwisunbae.common.time;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public final class DatabaseTime {

    private DatabaseTime() {
    }

    public static Instant normalize(final Instant instant) {
        return Objects.requireNonNull(instant).truncatedTo(ChronoUnit.MICROS);
    }
}
