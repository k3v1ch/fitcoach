package ru.sportorg.dashboard;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.sportorg.auth.AuthenticatedUser;
import ru.sportorg.organizations.OrganizationNotFoundException;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
    @Mock private DashboardRepository repository;
    private DashboardService service;
    private UUID organizationId;
    private UUID userId;
    private AuthenticatedUser parent;

    @BeforeEach
    void setUp() {
        service = new DashboardService(repository);
        organizationId = UUID.randomUUID(); userId = UUID.randomUUID();
        parent = new AuthenticatedUser(userId, "parent@example.org", "parent@example.org", "Parent", null, "hash", "USER", "ACTIVE", true);
        when(repository.organizationExists(organizationId)).thenReturn(true);
        when(repository.membership(userId, organizationId)).thenReturn(new DashboardRepository.Membership("[\"PARENT\"]", "[\"charges.read\"]"));
    }

    @Test
    void parentCannotOpenDashboardForForeignAthlete() {
        UUID athleteId = UUID.randomUUID();
        when(repository.athleteVisible(organizationId, athleteId, userId)).thenReturn(false);
        assertThrows(OrganizationNotFoundException.class, () -> service.get(parent, organizationId, "PARENT", athleteId,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), 0, 20));
    }
}