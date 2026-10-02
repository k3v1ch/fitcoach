package ru.sportorg.dashboard;

import java.time.Instant;
import java.util.UUID;

public record DashboardTraining(UUID id, String title, UUID groupId, Instant startsAt, Instant endsAt, String status) {
}