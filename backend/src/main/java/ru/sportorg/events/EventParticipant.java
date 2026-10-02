package ru.sportorg.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

record EventParticipant(UUID athleteId, String athleteName, String response, UUID respondedBy,
                        Instant respondedAt, String comment, List<UUID> documentIds) {
}