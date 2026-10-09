package ru.sportorg.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.sportorg.auth.AuthenticatedUser;
import ru.sportorg.organizations.OrganizationNotFoundException;
import ru.sportorg.organizations.OrganizationRequestException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-03T10:00:00Z");

    @Mock private NotificationRepository repository;
    private NotificationService service;
    private UUID org;
    private UUID userId;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        service = new NotificationService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        org = UUID.randomUUID();
        userId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        when(repository.member(org, userId)).thenReturn(true);
    }

    private AuthenticatedUser user(UUID id) {
        return new AuthenticatedUser(id, "u@example.org", "u@example.org", "User", null, "hash", "USER", "ACTIVE", true);
    }

    @Test
    void listShowsOwnNotificationsWithUnreadCount() {
        when(repository.count(org, userId, false)).thenReturn(3L);
        when(repository.unreadCount(org, userId)).thenReturn(2L);
        when(repository.find(org, userId, false, 20, 0)).thenReturn(List.of());

        NotificationPage page = service.list(user(userId), org, false, 0, 20);

        assertEquals(3, page.totalElements());
        assertEquals(2, page.unreadCount());
        assertEquals(1, page.totalPages());
    }

    @Test
    void nonMemberSeesNothing() {
        UUID stranger = UUID.randomUUID();
        when(repository.member(org, stranger)).thenReturn(false);

        assertThrows(OrganizationNotFoundException.class, () -> service.list(user(stranger), org, false, 0, 20));
        assertThrows(OrganizationNotFoundException.class, () -> service.readAll(user(stranger), org));
    }

    @Test
    void pageSizeIsLimited() {
        assertThrows(OrganizationRequestException.class, () -> service.list(user(userId), org, false, 0, 101));
    }

    @Test
    void foreignNotificationCannotBeMarkedRead() {
        UUID id = UUID.randomUUID();
        when(repository.markRead(org, userId, id, NOW)).thenReturn(0);

        assertThrows(OrganizationNotFoundException.class, () -> service.read(user(userId), org, id));
    }

    @Test
    void actorIsNotNotifiedAboutOwnChange() {
        UUID entity = UUID.randomUUID();

        service.notifyUsers(org, List.of(actorId), actorId, "TRAINING_CANCELLED", "Тренировка отменена", "текст", "TRAINING", entity);
        verify(repository, never()).insert(any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean());

        service.notifyUsers(org, List.of(actorId, userId, userId), actorId, "TRAINING_CANCELLED", "Тренировка отменена", "текст", "TRAINING", entity);
        verify(repository).insert(eq(org), eq(List.of(userId)), eq("TRAINING_CANCELLED"), eq("Тренировка отменена"), eq("текст"),
                eq("TRAINING"), eq(entity), eq(NOW), eq(false));
    }
}
