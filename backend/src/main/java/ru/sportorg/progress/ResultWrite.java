package ru.sportorg.progress;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ResultWrite(@NotBlank String metricName, @NotNull BigDecimal value, @NotBlank String unit,
                          @NotNull LocalDate measuredOn, String comment, boolean isPersonalBest) {
}