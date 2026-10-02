package ru.sportorg.dashboard;

import java.time.Instant;
import java.util.UUID;

public record Activity(UUID id, UUID actorId, String actorName, String action, String entityType,
                       UUID entityId, String title, Instant createdAt) {
}