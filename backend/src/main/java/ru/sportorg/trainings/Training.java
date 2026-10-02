package ru.sportorg.trainings;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

record Training(UUID id, UUID organizationId, String title, UUID groupId, List<UUID> coachIds, UUID venueId,
                UUID typeId, Instant startsAt, Instant endsAt, List<TrainingStage> plan, String comment,
                String status, String cancelReason, Instant createdAt, Instant updatedAt) {
}