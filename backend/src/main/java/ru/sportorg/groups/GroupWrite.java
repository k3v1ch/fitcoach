package ru.sportorg.groups;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record GroupWrite(@NotBlank String name, @NotNull UUID sectionId, @NotEmpty List<UUID> coachIds,
                         String description, @NotBlank String status) {
}