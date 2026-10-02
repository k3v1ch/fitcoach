package ru.sportorg.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

record Payment(UUID id, UUID organizationId, UUID chargeId, UUID athleteId, UUID sectionId,
               BigDecimal amount, LocalDate paidOn, String method, String comment, String status,
               UUID createdBy, Instant createdAt, UUID voidedBy, Instant voidedAt, String voidReason) {
}