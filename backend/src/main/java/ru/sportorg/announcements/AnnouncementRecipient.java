package ru.sportorg.announcements;

import java.time.Instant;
import java.util.UUID;

/** Получатель объявления: прочтение и последний ответ. */
record AnnouncementRecipient(UUID userId, String fullName, Instant readAt, String response, Instant respondedAt) {
}
