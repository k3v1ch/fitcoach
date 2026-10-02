package ru.sportorg.trainings;

import java.time.Instant;
import java.util.UUID;

record TrainingReport(UUID trainingId, String topic, String actualContent, String comment, String status,
                      UUID authorId, Instant updatedAt, Instant closedAt) {
}