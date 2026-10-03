package ru.sportorg.trainings;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
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

/** ТЗ: изменение и отмена тренировки с автоматическим уведомлением участников. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TrainingNotificationTest {
    private static final Instant NOW = Instant.parse("2026-10-03T10:00:00Z");

    @Mock private TrainingRepository repository;
    @Mock private NotificationService notifications;
    private TrainingService service;
    private UUID org;
    private UUID id;
    private UUID trainerId;
    private UUID parentId;
    private Training current;

    @BeforeEach
    void setUp() {
        service = new TrainingService(repository, new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC), notifications);
        org = UUID.randomUUID();
        id = UUID.randomUUID();
        trainerId = UUID.randomUUID();
        parentId = UUID.randomUUID();
        current = new Training(id, org, "ОФП", UUID.randomUUID(), List.of(trainerId), UUID.randomUUID(), UUID.randomUUID(),
                Instant.parse("2026-10-05T07:00:00Z"), Instant.parse("2026-10-05T08:30:00Z"), List.of(), null, "PLANNED",
                null, NOW, NOW);
        when(repository.organizationExists(org)).thenReturn(true);
        when(repository.membership(trainerId, org)).thenReturn(Optional.of(new TrainingRepository.MembershipAccess(
                List.of("TRAINER"), List.of("schedule.write"))));
        when(repository.findById(org, id)).thenReturn(Optional.of(current));
        when(repository.groupActive(any(), eq(org))).thenReturn(true);
        when(notifications.trainingAudience(id)).thenReturn(List.of(parentId));
        when(notifications.when(eq(org), any())).thenReturn("5 октября, 10:00");
    }

    private AuthenticatedUser trainer() {
        return new AuthenticatedUser(trainerId, "t@example.org", "t@example.org", "Trainer", null, "hash", "USER", "ACTIVE", true);
    }

    private TrainingPatch patch(String json) throws Exception {
        return new ObjectMapper().findAndRegisterModules().readValue(json, TrainingPatch.class);
    }

    @Test
    void cancellationNotifiesParticipantsWithReason() throws Exception {
        service.patch(trainer(), org, id, patch("{\"status\":\"CANCELLED\",\"cancelReason\":\"болезнь тренера\"}"));

        verify(notifications).notifyUsers(eq(org), argThat((Collection<UUID> users) -> users.contains(parentId)), eq(trainerId),
                eq("TRAINING_CANCELLED"), eq("Тренировка отменена"), contains("Причина: болезнь тренера"), eq("TRAINING"), eq(id));
    }

    @Test
    void rescheduleNotifiesParticipants() throws Exception {
        service.patch(trainer(), org, id, patch("{\"startsAt\":\"2026-10-06T07:00:00Z\",\"endsAt\":\"2026-10-06T08:30:00Z\"}"));

        verify(notifications).notifyUsers(eq(org), argThat((Collection<UUID> users) -> users.contains(parentId)), eq(trainerId),
                eq("TRAINING_CHANGED"), eq("Тренировка перенесена"), any(), eq("TRAINING"), eq(id));
    }

    @Test
    void venueChangeIsReported() throws Exception {
        service.patch(trainer(), org, id, patch("{\"venueId\":\"" + UUID.randomUUID() + "\"}"));

        verify(notifications).notifyUsers(eq(org), any(), eq(trainerId), eq("TRAINING_CHANGED"), eq("Изменения в тренировке"),
                contains("место проведения"), eq("TRAINING"), eq(id));
    }

    @Test
    void groupChangeNotifiesOldAndNewGroup() throws Exception {
        UUID newParent = UUID.randomUUID();
        when(notifications.trainingAudience(id)).thenReturn(List.of(parentId), List.of(newParent));

        service.patch(trainer(), org, id, patch("{\"groupId\":\"" + UUID.randomUUID() + "\"}"));

        verify(notifications).notifyUsers(eq(org), argThat((Collection<UUID> users) -> users.containsAll(List.of(parentId, newParent))),
                eq(trainerId), eq("TRAINING_CHANGED"), any(), any(), eq("TRAINING"), eq(id));
    }

    @Test
    void cosmeticChangeIsSilent() throws Exception {
        service.patch(trainer(), org, id, patch("{\"title\":\"ОФП и растяжка\",\"comment\":\"взять скакалки\"}"));

        verify(notifications, never()).notifyUsers(any(), any(), any(), any(), any(), any(), any(), any());
    }
}
