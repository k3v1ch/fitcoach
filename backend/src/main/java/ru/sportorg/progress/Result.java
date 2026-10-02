package ru.sportorg.progress;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

record Result(UUID id, UUID athleteId, String metricName, BigDecimal value, String unit, LocalDate measuredOn,
              String comment, boolean isPersonalBest, UUID createdBy, Instant createdAt, Instant updatedAt) {
}