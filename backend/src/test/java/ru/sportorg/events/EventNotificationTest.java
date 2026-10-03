package ru.sportorg.events;

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

/** ТЗ Ф7.7: уведомления о мероприятии — публикация (приглашение), новые участники, отмена. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventNotificationTest {
    private static final Instant NOW = Instant.parse("2026-10-03T10:00:00Z");

    @Mock private EventRepository repository;
    @Mock private NotificationService notifications;
    private EventService service;
    private UUID org;
    private UUID id;
    private UUID trainerId;
    private UUID parentId;

    @BeforeEach
    void setUp() {
        service = new EventService(repository, new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC), notifications);
        org = UUID.randomUUID();
        id = UUID.randomUUID();
        trainerId = UUID.randomUUID();
        parentId = UUID.randomUUID();
        when(repository.organizationExists(org)).thenReturn(true);
        when(repository.membership(trainerId, org)).thenReturn(Optional.of(new EventRepository.MembershipAccess(
                List.of("TRAINER"), List.of("events.read", "events.write"))));
        when(notifications.eventAudience(id)).thenReturn(List.of(parentId));
        when(repository.athleteExists(eq(org), any())).thenReturn(true);
    }

    private Event event(String status) {
        return new Event(id, org, "Сборы в Сочи", "CAMP", UUID.randomUUID(), null, Instant.parse("2026-10-20T06:00:00Z"),
                Instant.parse("2026-10-27T06:00:00Z"), "Сочи", null, null, null, null, List.of(), status, trainerId, NOW, NOW);
    }

    private AuthenticatedUser trainer() {
        return new AuthenticatedUser(trainerId, "t@example.org", "t@example.org", "Trainer", null, "hash", "USER", "ACTIVE", true);
    }

    private EventPatch patch(String json) throws Exception {
        return new ObjectMapper().findAndRegisterModules().readValue(json, EventPatch.class);
    }

    @Test
    void publishingInvitesParticipants() throws Exception {
        when(repository.findById(org, id)).thenReturn(Optional.of(event("DRAFT")), Optional.of(event("PUBLISHED")));

        service.patch(trainer(), org, id, patch("{\"status\":\"PUBLISHED\"}"));

        verify(notifications).notifyUsers(eq(org), eq(List.of(parentId)), eq(trainerId), eq("EVENT_PUBLISHED"), any(), any(),
                eq("EVENT"), eq(id));
    }

    @Test
    void cancellingPublishedNotifies() throws Exception {
        when(repository.findById(org, id)).thenReturn(Optional.of(event("PUBLISHED")), Optional.of(event("CANCELLED")));

        service.patch(trainer(), org, id, patch("{\"status\":\"CANCELLED\"}"));

        verify(notifications).notifyUsers(eq(org), eq(List.of(parentId)), eq(trainerId), eq("EVENT_CANCELLED"), any(), any(),
                eq("EVENT"), eq(id));
    }

    @Test
    void draftEditsAreSilent() throws Exception {
        when(repository.findById(org, id)).thenReturn(Optional.of(event("DRAFT")));

        service.patch(trainer(), org, id, patch("{\"title\":\"Сборы в Адлере\"}"));

        verify(notifications, never()).notifyUsers(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void newParticipantsOfPublishedEventAreInvited() {
        UUID already = UUID.randomUUID();
        UUID added = UUID.randomUUID();
        UUID addedParent = UUID.randomUUID();
        when(repository.findById(org, id)).thenReturn(Optional.of(event("PUBLISHED")));
        when(repository.participant(id, already)).thenReturn(true);
        when(repository.participant(id, added)).thenReturn(false);
        when(notifications.athletesAudience(List.of(added))).thenReturn(List.of(addedParent));

        service.addParticipants(trainer(), org, id, List.of(already, added));

        verify(notifications).notifyUsers(eq(org), eq(List.of(addedParent)), eq(trainerId), eq("EVENT_INVITED"), any(), any(),
                eq("EVENT"), eq(id));
    }

    @Test
    void participantsOfDraftAreNotNotifiedYet() {
        when(repository.findById(org, id)).thenReturn(Optional.of(event("DRAFT")));

        service.addParticipants(trainer(), org, id, List.of(UUID.randomUUID()));

        verify(notifications, never()).notifyUsers(any(), any(), any(), any(), any(), any(), any(), any());
    }
}
