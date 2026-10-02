package ru.sportorg.trainings;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record TrainingWrite(@NotBlank String title, @NotNull UUID groupId, @NotEmpty List<UUID> coachIds,
                            @NotNull UUID venueId, @NotNull UUID typeId, @NotNull OffsetDateTime startsAt,
                            @NotNull OffsetDateTime endsAt, @NotNull List<TrainingStage> plan, String comment) {
}