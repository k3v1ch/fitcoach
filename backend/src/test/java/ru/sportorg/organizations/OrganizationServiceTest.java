package ru.sportorg.organizations;

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
import ru.sportorg.auth.AuthenticatedUser;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-01T00:00:00Z");

    @Mock
    private OrganizationRepository repository;

    private OrganizationService organizationService;
    private UUID organizationId;
    private AuthenticatedUser trainer;

    @BeforeEach
    void setUp() {
        organizationService = new OrganizationService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        organizationId = UUID.randomUUID();
        trainer = new AuthenticatedUser(UUID.randomUUID(), "trainer@example.org", "trainer@example.org",
                "Trainer", null, "hash", "USER", "ACTIVE", true);
    }

    @Test
    void hidesOrganizationWhenUserHasNoActiveMembership() {
        when(repository.findOrganization(organizationId)).thenReturn(Optional.of(organization()));
        when(repository.findActiveMembership(trainer.userId(), organizationId)).thenReturn(Optional.empty());

        assertThrows(OrganizationNotFoundException.class,
                () -> organizationService.getOrganization(trainer, organizationId));
    }

    @Test
    void organizationWriteRequiresTrainerRoleAndPermission() {
        when(repository.findOrganization(organizationId)).thenReturn(Optional.of(organization()));
        when(repository.findActiveMembership(trainer.userId(), organizationId))
                .thenReturn(Optional.of(new OrganizationRepository.MembershipAccess(
                        List.of("AGENCY"), List.of("organization.write"))));
        var patch = new OrganizationPatch();
        patch.setName("Changed name");

        assertThrows(OrganizationPermissionException.class,
                () -> organizationService.patchOrganization(trainer, organizationId, patch));

        verify(repository, never()).patchOrganization(any(), any(), any());
    }

    @Test
    void trainerWithWritePermissionCanUpdateOrganization() {
        Organization organization = organization();
        when(repository.findOrganization(organizationId)).thenReturn(Optional.of(organization));
        when(repository.findActiveMembership(trainer.userId(), organizationId))
                .thenReturn(Optional.of(new OrganizationRepository.MembershipAccess(
                        List.of("TRAINER"), List.of("organization.write"))));
        var patch = new OrganizationPatch();
        patch.setName("Updated Club");

        organizationService.patchOrganization(trainer, organizationId, patch);

        verify(repository).patchOrganization(eq(organizationId), eq(patch), eq(NOW));
    }

    @Test
    void rejectsFixedOffsetInsteadOfIanaTimezone() {
        when(repository.findOrganization(organizationId)).thenReturn(Optional.of(organization()));
        when(repository.findActiveMembership(trainer.userId(), organizationId))
                .thenReturn(Optional.of(new OrganizationRepository.MembershipAccess(
                        List.of("TRAINER"), List.of("organization.write"))));
        var patch = new OrganizationPatch();
        patch.setTimezone("+03:00");

        assertThrows(OrganizationRequestException.class,
                () -> organizationService.patchOrganization(trainer, organizationId, patch));

        verify(repository, never()).patchOrganization(any(), any(), any());
    }

    @Test
    void memberDirectoryRequiresMembersRead() {
        when(repository.findOrganization(organizationId)).thenReturn(Optional.of(organization()));
        when(repository.findActiveMembership(trainer.userId(), organizationId))
                .thenReturn(Optional.of(new OrganizationRepository.MembershipAccess(
                        List.of("PARENT"), List.of("groups.read"))));

        assertThrows(OrganizationPermissionException.class,
                () -> organizationService.getMembers(trainer, organizationId, null, null, null, 0, 20));

        verify(repository, never()).countMembers(any(), any(), any(), any());
    }

    private Organization organization() {
        return new Organization(organizationId, "Club", null, null, "Europe/Moscow", NOW, NOW);
    }
}
