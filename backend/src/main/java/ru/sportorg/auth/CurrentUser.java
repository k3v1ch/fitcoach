package ru.sportorg.auth;

import java.time.Instant;
import java.util.UUID;

public record CurrentUser(UUID userId, String email, String fullName, String appRole, Instant expiresAt) {
}