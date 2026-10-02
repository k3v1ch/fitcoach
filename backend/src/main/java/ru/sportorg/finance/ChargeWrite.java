package ru.sportorg.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ChargeWrite(UUID athleteId, UUID sectionId, String type, String title, BigDecimal amount,
                          LocalDate dueOn, LocalDate periodFrom, LocalDate periodTo, UUID trainingId,
                          UUID eventId, String comment) {
}