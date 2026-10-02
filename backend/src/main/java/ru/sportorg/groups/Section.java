package ru.sportorg.groups;

import java.time.Instant;
import java.util.UUID;

record Section(UUID id, UUID organizationId, String name, UUID sportTypeId, String description,
               String status, Instant createdAt, Instant updatedAt) {
}