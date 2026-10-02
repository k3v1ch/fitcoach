package ru.sportorg.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentWrite(UUID chargeId, BigDecimal amount, LocalDate paidOn, String method, String comment) {
}