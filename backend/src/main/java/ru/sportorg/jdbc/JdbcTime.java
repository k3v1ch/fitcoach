package ru.sportorg.jdbc;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class JdbcTime {

    private JdbcTime() {
    }

    public static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}