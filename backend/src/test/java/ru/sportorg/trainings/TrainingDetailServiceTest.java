package ru.sportorg.trainings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import ru.sportorg.organizations.OrganizationNotFoundException;
import ru.sportorg.organizations.OrganizationPermissionException;

/** GET /trainings/{id} → TrainingDetail (6.8, №029): разделы по правам, родитель — только свои дети. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TrainingDetailServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-03T10:00:00Z");

    @Mock private TrainingRepository repository;
    private TrainingService service;
    private UUID org;
    private UUID id;
    private UUID trainerId;
    private UUID parentId;
    private UUID ownChild;
    private UUID otherChild;
    private Training training;
    private TrainingReport report;

    @BeforeEach
    void setUp() {
        service = new TrainingService(repository, new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
        org = UUID.randomUUID();
        id = UUID.randomUUID();
        trainerId = UUID.randomUUID();
        parentId = UUID.randomUUID();
        ownChild = UUID.randomUUID();
        otherChild = UUID.randomUUID();
        training = new Training(id, org, "ОФП", UUID.randomUUID(), List.of(trainerId), UUID.randomUUID(), UUID.randomUUID(),
                Instant.parse("2026-10-01T10:00:00Z"), Instant.parse("2026-10-01T11:30:00Z"), List.of(), null, "PLANNED",
                null, NOW, NOW);
        report = new TrainingReport(id, "Техника", "Отработали старты", null, "DRAFT", trainerId, NOW, null);
        when(repository.organizationExists(org)).thenReturn(true);
        when(repository.findById(org, id)).thenReturn(Optional.of(training));
        when(repository.report(id)).thenReturn(Optional.of(report));
        when(repository.attendanceWithParticipants(id)).thenReturn(List.of(
                new Attendance(id, ownChild, "Илья Смирнов", "PRESENT", null, null, trainerId, NOW),
                new Attendance(id, otherChild, "Анна Кузнецова", "UNMARKED", null, null, null, null)));
        when(repository.ownAthleteIds(org, parentId)).thenReturn(List.of(ownChild));
        when(repository.membership(trainerId, org)).thenReturn(Optional.of(new TrainingRepository.MembershipAccess(
                List.of("TRAINER"), List.of("schedule.read", "trainingReports.read", "attendance.read"))));
        when(repository.membership(parentId, org)).thenReturn(Optional.of(new TrainingRepository.MembershipAccess(
                List.of("PARENT"), List.of("schedule.read", "trainingReports.read", "attendance.read"))));
    }

    private AuthenticatedUser user(UUID userId) {
        return new AuthenticatedUser(userId, "u@example.org", "u@example.org", "User", null, "hash", "USER", "ACTIVE", true);
    }

    @Test
    void trainerGetsTrainingReportAndWholeComposition() {
        TrainingDetail detail = service.detail(user(trainerId), org, id);

        assertEquals(id, detail.trainingId());
        assertEquals(training, detail.training());
        assertEquals(report, detail.report());
        assertEquals(2, detail.attendance().size());
    }

    @Test
    void sectionWithoutPermissionIsNull() {
        when(repository.membership(trainerId, org)).thenReturn(Optional.of(new TrainingRepository.MembershipAccess(
                List.of("TRAINER"), List.of("schedule.read"))));

        TrainingDetail detail = service.detail(user(trainerId), org, id);

        assertEquals(training, detail.training());
        assertNull(detail.report());
        assertNull(detail.attendance());
    }

    @Test
    void noSectionPermissionIsForbidden() {
        when(repository.membership(trainerId, org)).thenReturn(Optional.of(new TrainingRepository.MembershipAccess(
                List.of("TRAINER"), List.of("groups.read"))));

        assertThrows(OrganizationPermissionException.class, () -> service.detail(user(trainerId), org, id));
    }

    @Test
    void parentSeesOnlyOwnChildMarks() {
        when(repository.visibleTo(org, id, parentId)).thenReturn(true);

        TrainingDetail detail = service.detail(user(parentId), org, id);

        assertEquals(List.of(ownChild), detail.attendance().stream().map(Attendance::athleteId).toList());
        assertEquals(report, detail.report());
    }

    @Test
    void parentCannotOpenForeignGroupTraining() {
        when(repository.visibleTo(org, id, parentId)).thenReturn(false);

        assertThrows(OrganizationNotFoundException.class, () -> service.detail(user(parentId), org, id));
    }

    @Test
    void unknownTrainingIsNotFound() {
        when(repository.findById(org, id)).thenReturn(Optional.empty());

        assertThrows(OrganizationNotFoundException.class, () -> service.detail(user(trainerId), org, id));
    }
}
