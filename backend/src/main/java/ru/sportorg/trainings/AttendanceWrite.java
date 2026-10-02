package ru.sportorg.trainings;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AttendanceWrite(@NotNull UUID athleteId, @NotNull String status, String reason, String comment) {
}