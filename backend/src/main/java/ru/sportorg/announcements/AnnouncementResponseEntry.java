package ru.sportorg.announcements;

import java.time.Instant;
import java.util.UUID;

/** Запись истории ответов на объявление (актуален последний ответ получателя). */
record AnnouncementResponseEntry(UUID id, UUID announcementId, UUID userId, String fullName, String response,
                                 String comment, Instant respondedAt) {
}
