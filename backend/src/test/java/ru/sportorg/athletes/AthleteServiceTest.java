package ru.sportorg.athletes;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
class AthleteServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-01T00:00:00Z");

    @Mock
    private AthleteRepository repository;

    private AthleteService service;
    private UUID organizationId;
    private UUID userId;
    private AuthenticatedUser actor;

    @BeforeEach
    void setUp() {
        service = new AthleteService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        actor = new AuthenticatedUser(userId, "parent@example.org", "parent@example.org", "Parent", null,
                "hash", "USER", "ACTIVE", true);
        when(repository.organizationExists(organizationId)).thenReturn(true);
    }

    @Test
    void organizationWithoutMembershipIsHidden() {
        when(repository.findActiveMembership(userId, organizationId)).thenReturn(Optional.empty());

        assertThrows(OrganizationNotFoundException.class,
                () -> service.find(actor, organizationId, null, null, 0, 20));
        verify(repository, never()).count(any(), any(), any(), any(), any());
    }

    @Test
    void parentWithoutReadPermissionCannotListAthletes() {
        when(repository.findActiveMembership(userId, organizationId)).thenReturn(Optional.of(
                new AthleteRepository.MembershipAccess(List.of("PARENT"), List.of("groups.read"))));

        assertThrows(OrganizationPermissionException.class,
                () -> service.find(actor, organizationId, null, null, 0, 20));
        verify(repository, never()).count(any(), any(), any(), any(), any());
    }

    @Test
    void parentLinkReplacementRequiresMembersWrite() {
        when(repository.findActiveMembership(userId, organizationId)).thenReturn(Optional.of(
                new AthleteRepository.MembershipAccess(List.of("TRAINER"), List.of("athletes.write"))));
        UUID athleteId = UUID.randomUUID();
        when(repository.findById(organizationId, athleteId)).thenReturn(Optional.of(new Athlete(
                athleteId, organizationId, "Ivan", "Petrov", null, LocalDate.of(2010, 1, 1), null,
                "ACTIVE", LocalDate.of(2024, 1, 1), null, List.of(), NOW, NOW)));
        AthletePatch patch = new AthletePatch();
        patch.setParentLinks(List.of());

        assertThrows(OrganizationPermissionException.class,
                () -> service.patch(actor, organizationId, athleteId, patch));
        verify(repository, never()).replaceParentLinks(any(), any(), any());
    }
}