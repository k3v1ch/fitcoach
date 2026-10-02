package ru.sportorg.events;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EventWrite(@NotBlank String title, @NotNull String type, @NotNull UUID sectionId, String description,
                         OffsetDateTime startsAt, OffsetDateTime endsAt, String location, BigDecimal costPerAthlete,
                         BigDecimal targetAmount, LocalDate collectionDueOn, OffsetDateTime responseDeadline,
                         @NotNull List<String> requiredDocumentTypes) {
}