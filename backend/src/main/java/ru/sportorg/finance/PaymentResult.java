package ru.sportorg.finance;

/** Платёж и пересчитанное начисление после изменения. */
record PaymentResult(Payment payment, Charge charge) {
}
