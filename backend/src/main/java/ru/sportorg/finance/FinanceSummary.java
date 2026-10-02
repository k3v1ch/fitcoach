package ru.sportorg.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

record FinanceSummary(LocalDate from, LocalDate to, String currency, BigDecimal receivedAmount,
                      BigDecimal outstandingAmount, BigDecimal overdueAmount) {
}