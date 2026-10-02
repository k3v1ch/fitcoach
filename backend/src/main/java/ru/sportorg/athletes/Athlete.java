package ru.sportorg.athletes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

record Athlete(UUID id, UUID organizationId, String firstName, String lastName, String middleName,
               LocalDate birthDate, UUID userId, String status, LocalDate enrolledOn, String note,
               List<ParentLink> parents, Instant createdAt, Instant updatedAt) {
}