package ru.sportorg.progress;

import java.time.LocalDate;
import java.util.UUID;

record AthleteStandard(UUID id, UUID athleteId, String name, String targetText, String resultText,
                       LocalDate assessedOn, String status, String comment) {
}