package ru.sportorg.documents;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

record Document(UUID id, UUID organizationId, UUID athleteId, String type, String title,
                StoredFile file, LocalDate issuedOn, LocalDate validUntil, UUID createdBy,
                Instant createdAt, Instant updatedAt) {
}