package ru.sportorg.announcements;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.sportorg.auth.AuthenticatedUser;
import ru.sportorg.notifications.NotificationService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnnouncementNotificationTest {
    private static final Instant NOW = Instant.parse("2026-10-03T10:00:00Z");

    @Mock private AnnouncementRepository repository;
    @Mock private NotificationService notifications;
    private AnnouncementService service;
    private UUID org;
    private UUID id;
    private UUID trainerId;
    private UUID parentId;

    @BeforeEach
    void setUp() {
        service = new AnnouncementService(repository, new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC), notifications);
        org = UUID.randomUUID();
        id = UUID.randomUUID();
        trainerId = UUID.randomUUID();
        parentId = UUID.randomUUID();
        when(repository.organizationExists(org)).thenReturn(true);
        when(repository.membership(trainerId, org)).thenReturn(Optional.of(new AnnouncementRepository.MembershipAccess(
                List.of("TRAINER"), List.of("announcements.read", "announcements.write"))));
        when(repository.member(eq(org), any())).thenReturn(true);
    }

    private Announcement announcement(String status) {
        return new Announcement(id, org, "Сбор", "Текст", null, false, null, List.of(), status, trainerId, null, NOW, NOW, null, null);
    }

    private AuthenticatedUser trainer() {
        return new AuthenticatedUser(trainerId, "t@example.org", "t@example.org", "Trainer", null, "hash", "USER", "ACTIVE", true);
    }

    private AnnouncementWrite write() {
        return new AnnouncementWrite("Сбор", "Текст", null, List.of(parentId), false, null, List.of());
    }

    @Test
    void publishingNotifiesRecipients() {
        when(repository.find(org, id, trainerId)).thenReturn(Optional.of(announcement("DRAFT")), Optional.of(announcement("PUBLISHED")));

        service.patch(trainer(), org, id, write(), "PUBLISHED");

        verify(notifications).notifyUsers(eq(org), eq(List.of(parentId)), eq(trainerId), eq("ANNOUNCEMENT_PUBLISHED"), any(), any(),
                eq("ANNOUNCEMENT"), eq(id));
    }

    @Test
    void savingDraftOrArchivingIsSilent() {
        when(repository.find(org, id, trainerId)).thenReturn(Optional.of(announcement("DRAFT")));
        service.patch(trainer(), org, id, write(), "DRAFT");

        when(repository.find(org, id, trainerId)).thenReturn(Optional.of(announcement("PUBLISHED")));
        service.patch(trainer(), org, id, write(), "ARCHIVED");

        verify(notifications, never()).notifyUsers(any(), any(), any(), any(), any(), any(), any(), any());
    }
}
