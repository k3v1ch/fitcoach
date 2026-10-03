package ru.sportorg.announcements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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
import ru.sportorg.organizations.OrganizationConflictException;
import ru.sportorg.organizations.OrganizationNotFoundException;
import ru.sportorg.organizations.OrganizationRequestException;

/** Видимость объявлений (6.12, №047, №049) и изменение после публикации (№050). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnnouncementVisibilityServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-03T00:00:00Z");

    @Mock private AnnouncementRepository repository;
    private AnnouncementService service;
    private UUID org;
    private UUID id;
    private UUID trainerId;
    private UUID parentId;
    private UUID agencyId;

    @BeforeEach
    void setUp() {
        service = new AnnouncementService(repository, new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
        org = UUID.randomUUID();
        id = UUID.randomUUID();
        trainerId = UUID.randomUUID();
        parentId = UUID.randomUUID();
        agencyId = UUID.randomUUID();
        when(repository.organizationExists(org)).thenReturn(true);
        when(repository.membership(trainerId, org)).thenReturn(Optional.of(new AnnouncementRepository.MembershipAccess(
                List.of("TRAINER"), List.of("announcements.read", "announcements.write"))));
        when(repository.membership(parentId, org)).thenReturn(Optional.of(new AnnouncementRepository.MembershipAccess(
                List.of("PARENT"), List.of("announcements.read"))));
        when(repository.membership(agencyId, org)).thenReturn(Optional.of(new AnnouncementRepository.MembershipAccess(
                List.of("AGENCY"), List.of("announcements.read"))));
        when(repository.findPage(any(), any(), any(), any(), anyBoolean(), any(), anyBoolean(), anyBoolean(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(repository.member(eq(org), any())).thenReturn(true);
    }

    private AuthenticatedUser user(UUID userId) {
        return new AuthenticatedUser(userId, "u@example.org", "u@example.org", "User", null, "hash", "USER", "ACTIVE", true);
    }

    private Announcement announcement(String status) {
        return new Announcement(id, org, "Сбор", "Текст", null, false, null, List.of(), status, trainerId,
                null, NOW, NOW, null, null);
    }

    private AnnouncementWrite write(List<UUID> recipients, List<UUID> files) {
        return new AnnouncementWrite("Сбор", "Текст", null, recipients, false, null, files);
    }

    @Test
    void recipientListShowsOnlyAddressedPublishedAndArchive() {
        service.find(user(parentId), org, null, null, false, null, 0, 20);

        verify(repository).count(org, null, parentId, null, false, null, false, false);
    }

    @Test
    void trainerListShowsDraftsAndEverythingPublished() {
        service.find(user(trainerId), org, null, null, false, null, 0, 20);

        verify(repository).count(org, null, trainerId, null, false, null, true, true);
    }

    @Test
    void agencySeesPublishedButNotDrafts() {
        service.find(user(agencyId), org, null, null, false, null, 0, 20);

        verify(repository).count(org, null, agencyId, null, false, null, false, true);
    }

    @Test
    void recipientCannotOpenDraft() {
        when(repository.find(org, id, parentId)).thenReturn(Optional.of(announcement("DRAFT")));
        when(repository.recipient(id, parentId)).thenReturn(true);

        assertThrows(OrganizationNotFoundException.class, () -> service.get(user(parentId), org, id));
    }

    @Test
    void publishedOpensOnlyForRecipients() {
        when(repository.find(org, id, parentId)).thenReturn(Optional.of(announcement("PUBLISHED")));
        when(repository.recipient(id, parentId)).thenReturn(false);
        assertThrows(OrganizationNotFoundException.class, () -> service.get(user(parentId), org, id));

        when(repository.recipient(id, parentId)).thenReturn(true);
        assertEquals(id, service.get(user(parentId), org, id).id());
    }

    @Test
    void trainerOpensAnyAnnouncement() {
        when(repository.find(org, id, trainerId)).thenReturn(Optional.of(announcement("PUBLISHED")));
        when(repository.recipient(id, trainerId)).thenReturn(false);

        assertEquals(id, service.get(user(trainerId), org, id).id());
    }

    @Test
    void archivingPublishedKeepsContentAndRecipients() {
        when(repository.find(org, id, trainerId)).thenReturn(Optional.of(announcement("PUBLISHED")));

        service.patch(user(trainerId), org, id, write(List.of(), List.of()), "ARCHIVED");

        verify(repository).archive(org, id, NOW);
        verify(repository, never()).patch(any(), any(), any(), any(), any(), any());
        verify(repository, never()).replaceRecipients(any(), any());
    }

    @Test
    void publishedCannotBeEdited() {
        when(repository.find(org, id, trainerId)).thenReturn(Optional.of(announcement("PUBLISHED")));

        assertThrows(OrganizationRequestException.class, () ->
                service.patch(user(trainerId), org, id, write(List.of(parentId), List.of()), "PUBLISHED"));
        verify(repository, never()).patch(any(), any(), any(), any(), any(), any());
    }

    @Test
    void archivedIsReadOnly() {
        when(repository.find(org, id, trainerId)).thenReturn(Optional.of(announcement("ARCHIVED")));

        assertThrows(OrganizationConflictException.class, () ->
                service.patch(user(trainerId), org, id, write(List.of(parentId), List.of()), "ARCHIVED"));
        verify(repository, never()).archive(any(), any(), any());
    }

    @Test
    void draftPatchStoresAttachmentsAsJson() {
        UUID file = UUID.randomUUID();
        when(repository.find(org, id, trainerId)).thenReturn(Optional.of(announcement("DRAFT")));

        service.patch(user(trainerId), org, id, write(List.of(parentId), List.of(file)), "PUBLISHED");

        verify(repository).patch(eq(org), eq(id), any(AnnouncementWrite.class), eq("[\"" + file + "\"]"), eq("PUBLISHED"), eq(NOW));
    }

    @Test
    void responseNeedsDeadlineOnDraftPatch() {
        when(repository.find(org, id, trainerId)).thenReturn(Optional.of(announcement("DRAFT")));

        assertThrows(OrganizationRequestException.class, () -> service.patch(user(trainerId), org, id,
                new AnnouncementWrite("Сбор", "Текст", null, List.of(parentId), true, null, List.of()), "PUBLISHED"));
        verify(repository, never()).patch(any(), any(), any(), any(), any(), any());
    }
}
