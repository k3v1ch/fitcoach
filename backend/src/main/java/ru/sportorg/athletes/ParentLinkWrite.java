package ru.sportorg.athletes;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ParentLinkWrite(@NotNull UUID parentUserId, String relationship) {
}