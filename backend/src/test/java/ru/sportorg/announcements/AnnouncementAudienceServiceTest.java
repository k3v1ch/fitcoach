package ru.sportorg.announcements;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnnouncementAudienceServiceTest {
    @Mock private AnnouncementRepository repository;
    private AnnouncementService service;
    private UUID org;
    private UUID announcementId;
    private UUID trainerId;
    private UUID parentId;

    @BeforeEach
    void setUp() {
        service = new AnnouncementService(repository, new ObjectMapper(), Clock.fixed(Instant.parse("2026-10-03T00:00:00Z"), ZoneOffset.UTC));
        org = UUID.randomUUID();
        announcementId = UUID.randomUUID();
        trainerId = UUID.randomUUID();
        parentId = UUID.randomUUID();
        when(repository.organizationExists(org)).thenReturn(true);
        when(repository.exists(org, announcementId)).thenReturn(true);
        when(repository.membership(trainerId, org)).thenReturn(Optional.of(
                new AnnouncementRepository.MembershipAccess(List.of("TRAINER"), List.of("announcements.read", "announcements.write"))));
        when(repository.membership(parentId, org)).thenReturn(Optional.of(
                new AnnouncementRepository.MembershipAccess(List.of("PARENT"), List.of("announcements.read"))));
        when(repository.recipients(any(), anyInt(), anyInt())).thenReturn(List.of());
        when(repository.responses(any(), any(), anyInt(), anyInt())).thenReturn(List.of());
    }

    private AuthenticatedUser user(UUID id) {
        return new AuthenticatedUser(id, "u@example.org", "u@example.org", "User", null, "hash", "USER", "ACTIVE", true);
    }

    @Test
    void onlyStaffSeesRecipients() {
        assertThrows(OrganizationPermissionException.class, () -> service.recipients(user(parentId), org, announcementId, 0, 20));
        verify(repository, never()).countRecipients(any());
    }

    @Test
    void missingAnnouncementIsNotFound() {
        UUID other = UUID.randomUUID();
        assertThrows(OrganizationNotFoundException.class, () -> service.recipients(user(trainerId), org, other, 0, 20));
        assertThrows(OrganizationNotFoundException.class, () -> service.responses(user(trainerId), org, other, 0, 20));
    }

    @Test
    void trainerSeesAllResponses() {
        service.responses(user(trainerId), org, announcementId, 0, 20);
        verify(repository).countResponses(eq(announcementId), isNull());
    }

    @Test
    void recipientSeesOnlyOwnResponses() {
        when(repository.recipient(announcementId, parentId)).thenReturn(true);
        service.responses(user(parentId), org, announcementId, 0, 20);
        verify(repository).countResponses(eq(announcementId), eq(parentId));
    }

    @Test
    void nonRecipientCannotSeeResponses() {
        when(repository.recipient(announcementId, parentId)).thenReturn(false);
        assertThrows(OrganizationNotFoundException.class, () -> service.responses(user(parentId), org, announcementId, 0, 20));
    }
}
