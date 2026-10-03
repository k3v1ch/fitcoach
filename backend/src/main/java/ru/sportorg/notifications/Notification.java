package ru.sportorg.notifications;

import java.time.Instant;
import java.util.UUID;

/** Уведомление пользователя; entityType — TRAINING | EVENT | ANNOUNCEMENT. */
public record Notification(UUID id, String type, String title, String text, String entityType, UUID entityId,
                           Instant createdAt, Instant readAt) {
}
