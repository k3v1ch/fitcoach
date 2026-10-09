package ru.sportorg.trainings;

import java.time.Instant;
import java.util.UUID;

/** Запись журнала посещаемости: отметка спортсмена и сведения о тренировке. */
record AttendanceEntry(UUID trainingId, String trainingTitle, UUID groupId, Instant startsAt, Instant endsAt,
                       UUID athleteId, String athleteName, String status, String reason, String comment,
                       UUID markedBy, Instant markedAt) {
}
