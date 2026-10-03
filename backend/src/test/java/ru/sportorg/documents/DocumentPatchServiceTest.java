package ru.sportorg.documents;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.sportorg.auth.AuthenticatedUser;
import ru.sportorg.organizations.OrganizationPermissionException;
import ru.sportorg.organizations.OrganizationRequestException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentPatchServiceTest {
    @Mock private DocumentRepository repository;
    @TempDir Path tempDir;
    private DocumentService service;
    private UUID org;
    private UUID ownerId;
    private UUID otherParentId;
    private UUID agencyId;
    private UUID athleteId;
    private Document document;

    @BeforeEach
    void setUp() {
        service = new DocumentService(repository, tempDir.toString(), Clock.fixed(Instant.parse("2026-10-03T00:00:00Z"), ZoneOffset.UTC));
        org = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        otherParentId = UUID.randomUUID();
        agencyId = UUID.randomUUID();
        athleteId = UUID.randomUUID();
        document = new Document(UUID.randomUUID(), org, athleteId, "MEDICAL_CERTIFICATE", "Справка", null,
                LocalDate.of(2026, 9, 1), LocalDate.of(2027, 9, 1), ownerId, Instant.now(), Instant.now());
        when(repository.organizationExists(org)).thenReturn(true);
        when(repository.findDocument(org, document.id())).thenReturn(Optional.of(document));
        when(repository.athleteVisible(eq(org), eq(athleteId), any())).thenReturn(true);
        when(repository.membership(ownerId, org)).thenReturn(new DocumentRepository.Membership("[\"PARENT\"]", "[\"documents.read\"]"));
        when(repository.membership(otherParentId, org)).thenReturn(new DocumentRepository.Membership("[\"PARENT\"]", "[\"documents.read\"]"));
        when(repository.membership(agencyId, org)).thenReturn(new DocumentRepository.Membership("[\"AGENCY\"]", "[\"documents.read\"]"));
    }

    private AuthenticatedUser user(UUID id) {
        return new AuthenticatedUser(id, "u@example.org", "u@example.org", "User", null, "hash", "USER", "ACTIVE", true);
    }

    private DocumentPatch patch(String json) throws Exception {
        return new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules().readValue(json, DocumentPatch.class);
    }

    @Test
    void ownerCanRenameOwnDocument() throws Exception {
        service.patch(user(ownerId), org, document.id(), patch("{\"title\":\"Справка 2026\"}"));
        verify(repository).patchDocument(eq(org), eq(document.id()), any(DocumentPatch.class), any());
    }

    @Test
    void otherParentWithoutWriteIsForbidden() throws Exception {
        assertThrows(OrganizationPermissionException.class,
                () -> service.patch(user(otherParentId), org, document.id(), patch("{\"title\":\"Чужое\"}")));
        verify(repository, never()).patchDocument(any(), any(), any(), any());
    }

    @Test
    void agencyCannotEditDocuments() throws Exception {
        when(repository.membership(agencyId, org)).thenReturn(new DocumentRepository.Membership("[\"AGENCY\"]", "[\"documents.read\",\"documents.write\"]"));
        assertThrows(OrganizationPermissionException.class,
                () -> service.patch(user(agencyId), org, document.id(), patch("{\"title\":\"Ведомство\"}")));
    }

    @Test
    void validityCannotEndBeforeIssue() throws Exception {
        assertThrows(OrganizationRequestException.class,
                () -> service.patch(user(ownerId), org, document.id(), patch("{\"validUntil\":\"2026-08-01\"}")));
        assertThrows(OrganizationRequestException.class,
                () -> service.patch(user(ownerId), org, document.id(), patch("{}")));
        assertThrows(OrganizationRequestException.class,
                () -> service.patch(user(ownerId), org, document.id(), patch("{\"title\":\"  \"}")));
    }
}
