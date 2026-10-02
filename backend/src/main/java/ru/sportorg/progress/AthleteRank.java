package ru.sportorg.progress;

import java.time.LocalDate;
import java.util.UUID;

record AthleteRank(UUID id, UUID athleteId, String name, UUID sportTypeId, LocalDate assignedOn,
                   LocalDate validUntil, String comment) {
}