package ru.sportorg.announcements;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.sportorg.auth.AuthenticatedUser;
import ru.sportorg.organizations.OrganizationPermissionException;
import ru.sportorg.organizations.OrganizationRequestException;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {
    @Mock private AnnouncementRepository repository;
    private AnnouncementService service;
    private UUID organizationId;
    private UUID userId;
    private AuthenticatedUser actor;

    @BeforeEach
    void setUp() {
        service = new AnnouncementService(repository, new ObjectMapper(), Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC));
        organizationId = UUID.randomUUID(); userId = UUID.randomUUID();
        actor = new AuthenticatedUser(userId, "parent@example.org", "parent@example.org", "Parent", null, "hash", "USER", "ACTIVE", true);
        when(repository.organizationExists(organizationId)).thenReturn(true);
    }

    @Test
    void parentCannotCreateAnnouncement() {
        when(repository.membership(userId, organizationId)).thenReturn(Optional.of(
                new AnnouncementRepository.MembershipAccess(List.of("PARENT"), List.of("announcements.write"))));
        assertThrows(OrganizationPermissionException.class, () -> service.create(actor, organizationId,
                new AnnouncementWrite("Title", "Text", null, List.of(UUID.randomUUID()), false, null, List.of())));
        verify(repository, never()).insert(any(), any(), any(), any(), any());
    }

    @Test
    void unknownRecipientIsRejected() {
        UUID recipient = UUID.randomUUID();
        when(repository.membership(userId, organizationId)).thenReturn(Optional.of(
                new AnnouncementRepository.MembershipAccess(List.of("TRAINER"), List.of("announcements.write"))));
        when(repository.member(eq(organizationId), eq(recipient))).thenReturn(false);
        assertThrows(OrganizationRequestException.class, () -> service.create(actor, organizationId,
                new AnnouncementWrite("Title", "Text", null, List.of(recipient), false, null, List.of())));
        verify(repository, never()).insert(any(), any(), any(), any(), any());
    }
}