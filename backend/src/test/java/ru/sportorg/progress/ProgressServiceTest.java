package ru.sportorg.progress;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import ru.sportorg.auth.AuthenticatedUser;
import ru.sportorg.organizations.OrganizationNotFoundException;
import ru.sportorg.organizations.OrganizationPermissionException;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {
    @Mock private ProgressRepository repository;
    private ProgressService service;
    private UUID organizationId;
    private UUID userId;
    private AuthenticatedUser actor;

    @BeforeEach
    void setUp() {
        service = new ProgressService(repository, Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC));
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        actor = new AuthenticatedUser(userId, "parent@example.org", "parent@example.org", "Parent", null,
                "hash", "USER", "ACTIVE", true);
        when(repository.organizationExists(organizationId)).thenReturn(true);
    }

    @Test
    void parentCannotReadForeignAthleteResults() {
        UUID athleteId = UUID.randomUUID();
        when(repository.membership(userId, organizationId)).thenReturn(Optional.of(
                new ProgressRepository.MembershipAccess(List.of("PARENT"), List.of("progress.read"))));
        when(repository.athleteExists(organizationId, athleteId)).thenReturn(true);
        when(repository.athleteVisible(organizationId, athleteId, userId, "PARENT")).thenReturn(false);

        assertThrows(OrganizationNotFoundException.class,
                () -> service.results(actor, organizationId, athleteId, null, null, null, null, 0, 20));
        verify(repository, never()).resultCount(any(), any(), any(), any(), any(), any());
    }

    @Test
    void agencyCannotWriteResult() {
        UUID athleteId = UUID.randomUUID();
        when(repository.membership(userId, organizationId)).thenReturn(Optional.of(
                new ProgressRepository.MembershipAccess(List.of("AGENCY"), List.of("progress.write"))));

        assertThrows(OrganizationPermissionException.class, () -> service.create(actor, organizationId, athleteId,
                new ResultWrite("Sprint", BigDecimal.ONE, "sec", LocalDate.of(2026, 10, 1), null, false)));
        verify(repository, never()).insertResult(any(), any(), any(), any(), any());
    }
}