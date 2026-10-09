package ru.sportorg.reports;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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

/** Родитель и спортсмен получают в отчётах только строки своих детей (себя), тренер и ведомство — всю организацию. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportScopeServiceTest {
    private static final LocalDate FROM = LocalDate.of(2026, 9, 1);
    private static final LocalDate TO = LocalDate.of(2026, 9, 30);
    private static final String SELF = "[\"attendance.read\",\"charges.read\",\"progress.read\",\"reports.export\",\"reports.read\",\"schedule.read\"]";

    @Mock private ReportRepository repository;
    private ReportService service;
    private UUID org;

    @BeforeEach
    void setUp() {
        service = new ReportService(repository);
        org = UUID.randomUUID();
        when(repository.organizationExists(org)).thenReturn(true);
        when(repository.rows(any(), any(), any(), any(), anyInt(), any())).thenReturn(List.of());
    }

    private AuthenticatedUser member(String roles, String permissions) {
        UUID id = UUID.randomUUID();
        when(repository.membership(id, org)).thenReturn(new ReportRepository.Membership(roles, permissions));
        return new AuthenticatedUser(id, "u@example.org", "u@example.org", "User", null, "hash", "USER", "ACTIVE", true);
    }

    @Test
    void parentSeesOnlyOwnChildrenRows() {
        AuthenticatedUser parent = member("[\"PARENT\"]", SELF);

        service.view(parent, org, ReportType.CHARGES, FROM, TO, 0, 20);
        service.export(parent, org, ReportType.ATTENDANCE, "CSV", FROM, TO);

        verify(repository).rows(eq(ReportType.CHARGES), eq(org), eq(FROM), eq(TO), anyInt(), eq(parent.userId()));
        verify(repository).rows(eq(ReportType.ATTENDANCE), eq(org), eq(FROM), eq(TO), anyInt(), eq(parent.userId()));
    }

    @Test
    void athleteSeesOnlyOwnRows() {
        AuthenticatedUser athlete = member("[\"ATHLETE\"]", SELF);

        service.view(athlete, org, ReportType.PROGRESS, FROM, TO, 0, 20);

        verify(repository).rows(eq(ReportType.PROGRESS), eq(org), eq(FROM), eq(TO), anyInt(), eq(athlete.userId()));
    }

    @Test
    void trainerAndAgencySeeWholeOrganization() {
        AuthenticatedUser trainer = member("[\"TRAINER\"]", SELF);
        AuthenticatedUser agency = member("[\"AGENCY\"]", SELF);
        AuthenticatedUser trainerParent = member("[\"PARENT\",\"TRAINER\"]", SELF);

        service.view(trainer, org, ReportType.TRAININGS, FROM, TO, 0, 20);
        service.view(agency, org, ReportType.ATTENDANCE, FROM, TO, 0, 20);
        service.view(trainerParent, org, ReportType.CHARGES, FROM, TO, 0, 20);

        verify(repository).rows(eq(ReportType.TRAININGS), eq(org), eq(FROM), eq(TO), anyInt(), isNull());
        verify(repository).rows(eq(ReportType.ATTENDANCE), eq(org), eq(FROM), eq(TO), anyInt(), isNull());
        verify(repository).rows(eq(ReportType.CHARGES), eq(org), eq(FROM), eq(TO), anyInt(), isNull());
    }
}
