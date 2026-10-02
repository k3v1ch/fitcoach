package ru.sportorg.athletes;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AthleteWrite(
        @NotBlank String firstName,
        @NotBlank String lastName,
        String middleName,
        @NotNull LocalDate birthDate,
        UUID userId,
        @NotBlank String status,
        @NotNull LocalDate enrolledOn,
        String note) {
}