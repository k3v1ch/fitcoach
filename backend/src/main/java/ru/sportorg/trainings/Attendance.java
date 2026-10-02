package ru.sportorg.trainings;

import java.time.Instant;
import java.util.UUID;

record Attendance(UUID trainingId, UUID athleteId, String athleteName, String status, String reason,
                  String comment, UUID markedBy, Instant markedAt) {
}