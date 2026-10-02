package ru.sportorg.announcements;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

record Announcement(
    UUID id, UUID organizationId, String title, String text, String category,
    boolean requiresResponse, Instant responseDeadline, List<UUID> attachmentFileIds,
    String status, UUID createdBy, Instant publishedAt, Instant createdAt, Instant updatedAt,
    Instant readAt, String myResponse
) { }