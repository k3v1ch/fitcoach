package ru.sportorg.groups;

import java.time.LocalDate;
import java.util.UUID;

record GroupAthlete(UUID athleteId, String fullName, LocalDate joinedOn, LocalDate leftOn) {
}