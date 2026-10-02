package ru.sportorg.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

record Event(UUID id, UUID organizationId, String title, String type, UUID sectionId, String description,
             Instant startsAt, Instant endsAt, String location, BigDecimal costPerAthlete, BigDecimal targetAmount,
             LocalDate collectionDueOn, Instant responseDeadline, List<String> requiredDocumentTypes, String status,
             UUID createdBy, Instant createdAt, Instant updatedAt) {
}