package ru.sportorg.dashboard;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DashboardEvent(UUID id, String title, String type, Instant startsAt, LocalDate collectionDueOn, String status) {
}