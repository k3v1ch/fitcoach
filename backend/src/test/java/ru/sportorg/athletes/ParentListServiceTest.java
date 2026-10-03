package ru.sportorg.athletes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import ru.sportorg.organizations.OrganizationPermissionException;
import ru.sportorg.organizations.OrganizationRequestException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ParentListServiceTest {
    @Mock private AthleteRepository repository;
    private AthleteService service;
    private UUID org;
    private UUID trainerId;
    private UUID parentId;

    @BeforeEach
    void setUp() {
        service = new AthleteService(repository, Clock.fixed(Instant.parse("2026-10-03T00:00:00Z"), ZoneOffset.UTC));
        org = UUID.randomUUID();
        trainerId = UUID.randomUUID();
        parentId = UUID.randomUUID();
        when(repository.organizationExists(org)).thenReturn(true);
        when(repository.findActiveMembership(trainerId, org)).thenReturn(Optional.of(
                new AthleteRepository.MembershipAccess(List.of("TRAINER"), List.of("athletes.read", "athletes.write"))));
        when(repository.findActiveMembership(parentId, org)).thenReturn(Optional.of(
                new AthleteRepository.MembershipAccess(List.of("PARENT"), List.of("athletes.read"))));
        when(repository.parents(any(), any(), any(), anyInt(), anyInt())).thenReturn(List.of());
    }

    private AuthenticatedUser user(UUID id) {
        return new AuthenticatedUser(id, "u@example.org", "u@example.org", "User", null, "hash", "USER", "ACTIVE", true);
    }

    @Test
    void parentCannotListAllParents() {
        assertThrows(OrganizationPermissionException.class, () -> service.parents(user(parentId), org, null, null, 0, 20));
        verify(repository, never()).countParents(any(), any(), any());
    }

    @Test
    void trainerSearchIsNormalized() {
        UUID athleteId = UUID.randomUUID();
        when(repository.countParents(eq(org), eq("иван"), eq(athleteId))).thenReturn(3L);

        ParentPage page = service.parents(user(trainerId), org, "  Иван ", athleteId, 0, 2);

        assertEquals(3L, page.totalElements());
        assertEquals(2, page.totalPages());
        verify(repository).parents(eq(org), eq("иван"), eq(athleteId), eq(2), eq(0));
    }

    @Test
    void pageSizeIsLimited() {
        assertThrows(OrganizationRequestException.class, () -> service.parents(user(trainerId), org, null, null, 0, 101));
    }
}
