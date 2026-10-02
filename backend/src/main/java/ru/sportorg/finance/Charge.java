package ru.sportorg.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

record Charge(UUID id, UUID organizationId, UUID athleteId, UUID sectionId, String type, String title,
              BigDecimal amount, LocalDate dueOn, BigDecimal paidAmount, BigDecimal remainingAmount,
              String paymentStatus, boolean overdue, String status) {
}