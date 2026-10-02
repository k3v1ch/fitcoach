package ru.sportorg.groups;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SectionWrite(@NotBlank String name, @NotNull UUID sportTypeId, String description,
                           @NotBlank String status) {
}