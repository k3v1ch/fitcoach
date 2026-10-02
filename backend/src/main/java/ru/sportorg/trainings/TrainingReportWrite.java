package ru.sportorg.trainings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TrainingReportWrite(@NotBlank String topic, @NotBlank String actualContent, String comment,
                                  @NotNull String status) {
}