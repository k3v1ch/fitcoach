package ru.sportorg.reports;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.sportorg.auth.AuthenticatedUser;
import ru.sportorg.organizations.OrganizationPermissionException;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {
    @Mock private ReportRepository repository;
    private ReportService service;
    private UUID organizationId;
    private UUID userId;
    private AuthenticatedUser actor;

    @BeforeEach
    void setUp() {
        service = new ReportService(repository);
        organizationId = UUID.randomUUID(); userId = UUID.randomUUID();
        actor = new AuthenticatedUser(userId, "trainer@example.org", "trainer@example.org", "Trainer", null, "hash", "USER", "ACTIVE", true);
        when(repository.organizationExists(organizationId)).thenReturn(true);
        when(repository.membership(userId, organizationId)).thenReturn(new ReportRepository.Membership("[\"TRAINER\"]", "[\"reports.read\",\"progress.read\",\"reports.export\"]"));
    }

    @Test
    void csvEscapesFormulaPrefix() {
        when(repository.rows(ReportType.PROGRESS, organizationId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), 10001))
                .thenReturn(List.of(Map.of("comment", "=SUM(A1:A2)")));

        String csv = new String(service.export(actor, organizationId, ReportType.PROGRESS, "CSV",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)));
        assertTrue(csv.contains("'=SUM(A1:A2)"));
    }

    @Test
    void exportRequiresExportPermission() {
        when(repository.membership(userId, organizationId)).thenReturn(new ReportRepository.Membership("[\"TRAINER\"]", "[\"reports.read\",\"progress.read\"]"));
        assertThrows(OrganizationPermissionException.class, () -> service.export(actor, organizationId, ReportType.PROGRESS, "CSV", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)));
    }
}