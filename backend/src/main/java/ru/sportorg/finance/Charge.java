package ru.sportorg.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

record Charge(UUID id, UUID organizationId, UUID athleteId, UUID sectionId, String type, String title,
              BigDecimal amount, LocalDate dueOn, BigDecimal paidAmount, BigDecimal remainingAmount,
              String paymentStatus, boolean overdue, String status,
              LocalDate periodFrom, LocalDate periodTo, UUID trainingId, UUID eventId, String comment,
              String cancelReason, UUID cancelledBy, Instant cancelledAt,
              UUID createdBy, Instant createdAt, Instant updatedAt) {

    /** Начисление без ссылок, комментария и служебных полей. */
    Charge(UUID id, UUID organizationId, UUID athleteId, UUID sectionId, String type, String title,
           BigDecimal amount, LocalDate dueOn, BigDecimal paidAmount, BigDecimal remainingAmount,
           String paymentStatus, boolean overdue, String status) {
        this(id, organizationId, athleteId, sectionId, type, title, amount, dueOn, paidAmount, remainingAmount,
                paymentStatus, overdue, status, null, null, null, null, null, null, null, null, null, null, null);
    }
}
