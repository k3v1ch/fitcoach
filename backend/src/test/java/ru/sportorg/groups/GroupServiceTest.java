package ru.sportorg.groups;

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
import ru.sportorg.organizations.OrganizationPermissionException;
import ru.sportorg.organizations.OrganizationRequestException;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-01T00:00:00Z");
    @Mock private GroupRepository repository;
    private GroupService service;
    private UUID organizationId;
    private UUID userId;
    private AuthenticatedUser actor;

    @BeforeEach
    void setUp() {
        service = new GroupService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        actor = new AuthenticatedUser(userId, "user@example.org", "user@example.org", "User", null,
                "hash", "USER", "ACTIVE", true);
        when(repository.organizationExists(organizationId)).thenReturn(true);
    }

    @Test
    void sectionListRequiresReadPermission() {
        when(repository.membership(userId, organizationId)).thenReturn(Optional.of(
                new GroupRepository.MembershipAccess(List.of("PARENT"), List.of("groups.read"))));

        assertThrows(OrganizationPermissionException.class,
                () -> service.sections(actor, organizationId, null, null, null, 0, 20));
        verify(repository, never()).countSections(any(), any(), any(), any());
    }

    @Test
    void parentCannotCreateGroup() {
        when(repository.membership(userId, organizationId)).thenReturn(Optional.of(
                new GroupRepository.MembershipAccess(List.of("PARENT"), List.of("groups.write"))));

        assertThrows(OrganizationPermissionException.class,
                () -> service.createGroup(actor, organizationId,
                        new GroupWrite("Group", UUID.randomUUID(), List.of(UUID.randomUUID()), null, "ACTIVE")));
        verify(repository, never()).insertGroup(any(), any(), any());
    }

    @Test
    void archivedGroupRejectsNewAthlete() {
        UUID groupId = UUID.randomUUID();
        UUID athleteId = UUID.randomUUID();
        when(repository.membership(userId, organizationId)).thenReturn(Optional.of(
                new GroupRepository.MembershipAccess(List.of("TRAINER"), List.of("groups.write"))));
        when(repository.athleteInOrg(athleteId, organizationId)).thenReturn(true);
        when(repository.group(organizationId, groupId)).thenReturn(Optional.of(new Group(
                groupId, organizationId, UUID.randomUUID(), "Old", List.of(), null, "ARCHIVED", 0, NOW, NOW)));

        assertThrows(OrganizationRequestException.class,
                () -> service.addAthlete(actor, organizationId, groupId, athleteId, LocalDate.of(2026, 10, 1)));
        verify(repository, never()).addAthlete(any(), any(), any(), any());
    }
}