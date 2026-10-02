package ru.sportorg.groups;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

record Group(UUID id, UUID organizationId, UUID sectionId, String name, List<UUID> coachIds,
             String description, String status, int athleteCount, Instant createdAt, Instant updatedAt) {
}