package ru.sportorg.trainings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
import ru.sportorg.organizations.OrganizationRequestException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AttendanceJournalServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-03T10:00:00Z");
    private static final LocalDate FROM = LocalDate.of(2026, 9, 1);
    private static final LocalDate TO = LocalDate.of(2026, 9, 30);

    @Mock private TrainingRepository repository;
    private TrainingService service;
    private UUID org;
    private UUID trainerId;
    private UUID parentId;
    private AuthenticatedUser trainer;
    private AuthenticatedUser parent;

    @BeforeEach
    void setUp() {
        service = new TrainingService(repository, new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
        org = UUID.randomUUID();
        trainerId = UUID.randomUUID();
        parentId = UUID.randomUUID();
        trainer = new AuthenticatedUser(trainerId, "t@example.org", "t@example.org", "Trainer", null, "hash", "USER", "ACTIVE", true);
        parent = new AuthenticatedUser(parentId, "p@example.org", "p@example.org", "Parent", null, "hash", "USER", "ACTIVE", true);
        when(repository.organizationExists(org)).thenReturn(true);
        when(repository.membership(trainerId, org)).thenReturn(Optional.of(new TrainingRepository.MembershipAccess(
                List.of("TRAINER"), List.of("schedule.read", "attendance.read", "attendance.write"))));
        when(repository.membership(parentId, org)).thenReturn(Optional.of(new TrainingRepository.MembershipAccess(
                List.of("PARENT"), List.of("schedule.read", "attendance.read"))));
        when(repository.journalSummary(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new TrainingRepository.JournalCounts(2, 6, 3, 1, 1, 1));
        when(repository.journal(any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(List.of());
    }

    @Test
    void journalNeedsValidPeriodAndStatus() {
        assertThrows(OrganizationRequestException.class, () -> service.attendanceJournal(trainer, org, null, TO, null, null, null, 0, 20));
        assertThrows(OrganizationRequestException.class, () -> service.attendanceJournal(trainer, org, TO, FROM, null, null, null, 0, 20));
        assertThrows(OrganizationRequestException.class, () -> service.attendanceJournal(trainer, org, FROM, TO, null, null, "LATE", 0, 20));
        assertThrows(OrganizationRequestException.class, () -> service.attendanceJournal(trainer, org, FROM, TO, null, null, null, 0, 101));
    }

    @Test
    void parentJournalIsLimitedToOwnChildren() {
        UUID athleteId = UUID.randomUUID();

        service.attendanceJournal(parent, org, FROM, TO, athleteId, null, null, 0, 20);

        verify(repository).journalSummary(eq(org), eq(FROM), eq(TO), eq(athleteId), isNull(), eq(parentId), eq(NOW));
        verify(repository).journalCount(eq(org), eq(FROM), eq(TO), eq(athleteId), isNull(), isNull(), eq(parentId), eq(NOW));
    }

    @Test
    void trainerJournalCoversOrganization() {
        service.attendanceJournal(trainer, org, FROM, TO, null, null, "ABSENT", 0, 20);

        verify(repository).journalCount(eq(org), eq(FROM), eq(TO), isNull(), isNull(), eq("ABSENT"), isNull(), eq(NOW));
    }

    @Test
    void summaryPercentIgnoresUnmarked() {
        AttendanceList list = service.attendanceJournal(trainer, org, FROM, TO, null, null, null, 0, 20);

        assertEquals(2, list.summary().trainingCount());
        assertEquals(6, list.summary().participantRecords());
        assertEquals(new BigDecimal("60.00"), list.summary().attendancePercent()); // 3 / (3 + 1 + 1)
    }

    @Test
    void percentIsNullWithoutMarks() {
        when(repository.journalSummary(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new TrainingRepository.JournalCounts(1, 4, 0, 0, 0, 4));

        assertNull(service.attendanceJournal(trainer, org, FROM, TO, null, null, null, 0, 20).summary().attendancePercent());
    }

    @Test
    void parentScheduleIsLimitedToOwnChildren() {
        Instant from = Instant.parse("2026-10-01T00:00:00Z");
        Instant to = Instant.parse("2026-10-08T00:00:00Z");

        service.find(parent, org, null, from, to, null, null, null, null, 0, 20);
        verify(repository).count(eq(org), isNull(), eq(from), eq(to), isNull(), isNull(), isNull(), isNull(), eq(parentId));

        UUID athleteId = UUID.randomUUID();
        service.find(trainer, org, null, from, to, null, null, athleteId, null, 0, 20);
        verify(repository).count(eq(org), isNull(), eq(from), eq(to), isNull(), isNull(), eq(athleteId), isNull(), isNull());
    }
}
