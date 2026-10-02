package ru.sportorg.announcements;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AnnouncementWrite(@NotBlank String title, @NotBlank String text, String category,
                                @NotNull List<UUID> recipientUserIds, boolean requiresResponse,
                                OffsetDateTime responseDeadline, @NotNull List<UUID> attachmentFileIds) {
}