package ru.sportorg.finance;

/** Аннулирование ошибочной записи платежа: причина обязательна. */
public record PaymentVoid(String reason) {
}
