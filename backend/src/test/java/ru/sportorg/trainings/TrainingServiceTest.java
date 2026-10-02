package ru.sportorg.trainings;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.sportorg.auth.AuthenticatedUser;
import ru.sportorg.organizations.OrganizationPermissionException;
import ru.sportorg.organizations.OrganizationRequestException;

@ExtendWith(MockitoExtension.class)
class TrainingServiceTest {
    @Mock private TrainingRepository repository;
    private TrainingService service;
    private UUID organizationId;
    private UUID userId;
    private AuthenticatedUser actor;

    @BeforeEach
    void setUp() {
        service = new TrainingService(repository, new ObjectMapper(), Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC));
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        actor = new AuthenticatedUser(userId, "parent@example.org", "parent@example.org", "Parent", null,
                "hash", "USER", "ACTIVE", true);
        when(repository.organizationExists(organizationId)).thenReturn(true);
    }

    @Test
    void scheduleRequiresReadPermission() {
        when(repository.membership(userId, organizationId)).thenReturn(Optional.of(
                new TrainingRepository.MembershipAccess(List.of("PARENT"), List.of("groups.read"))));

        assertThrows(OrganizationPermissionException.class,
                () -> service.find(actor, organizationId, null, Instant.parse("2026-10-01T00:00:00Z"),
                        Instant.parse("2026-10-02T00:00:00Z"), null, null, null, 0, 20));
        verify(repository, never()).count(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void planCannotExceedTrainingDuration() {
        UUID groupId = UUID.randomUUID();
        UUID coachId = UUID.randomUUID();
        when(repository.membership(userId, organizationId)).thenReturn(Optional.of(
                new TrainingRepository.MembershipAccess(List.of("TRAINER"), List.of("schedule.write"))));
        when(repository.groupActive(groupId, organizationId)).thenReturn(true);
        when(repository.activeCoach(coachId, organizationId)).thenReturn(true);
        TrainingWrite write = new TrainingWrite("Morning", groupId, List.of(coachId), UUID.randomUUID(), UUID.randomUUID(),
                OffsetDateTime.parse("2026-10-01T10:00:00+03:00"), OffsetDateTime.parse("2026-10-01T10:30:00+03:00"),
                List.of(new TrainingStage("Warmup", 31, null)), null);

        assertThrows(OrganizationRequestException.class, () -> service.create(actor, organizationId, write));
        verify(repository, never()).insert(any(), any(), any(), any());
    }

        @Test
        void attendanceRejectsAthleteOutsideTrainingComposition() {
                UUID trainingId = UUID.randomUUID();
                UUID participantId = UUID.randomUUID();
                UUID foreignAthleteId = UUID.randomUUID();
                when(repository.membership(userId, organizationId)).thenReturn(Optional.of(
                                new TrainingRepository.MembershipAccess(List.of("TRAINER"), List.of("attendance.write"))));
                when(repository.findById(organizationId, trainingId)).thenReturn(Optional.of(new Training(
                                trainingId, organizationId, "Training", UUID.randomUUID(), List.of(), UUID.randomUUID(),
                                UUID.randomUUID(), Instant.parse("2026-09-30T10:00:00Z"), Instant.parse("2026-09-30T11:00:00Z"),
                                List.of(), null, "PLANNED", null, Instant.parse("2026-09-29T00:00:00Z"),
                                Instant.parse("2026-09-29T00:00:00Z"))));
                when(repository.participantIds(trainingId)).thenReturn(List.of(participantId));

                assertThrows(OrganizationRequestException.class, () -> service.saveAttendance(actor, organizationId,
                                trainingId, List.of(new AttendanceWrite(foreignAthleteId, "PRESENT", null, null))));
                verify(repository, never()).saveAttendance(any(), any(), any(), any());
        }
}