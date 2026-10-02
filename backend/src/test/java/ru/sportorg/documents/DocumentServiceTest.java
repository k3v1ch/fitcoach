package ru.sportorg.documents;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import ru.sportorg.auth.AuthenticatedUser;
import ru.sportorg.organizations.OrganizationRequestException;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {
    @Mock private DocumentRepository repository;
    @TempDir Path tempDir;
    private DocumentService service;
    private UUID organizationId;
    private UUID userId;
    private AuthenticatedUser actor;

    @BeforeEach
    void setUp() {
        service = new DocumentService(repository, tempDir.toString(), Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC));
        organizationId = UUID.randomUUID(); userId = UUID.randomUUID();
        actor = new AuthenticatedUser(userId, "trainer@example.org", "trainer@example.org", "Trainer", null, "hash", "USER", "ACTIVE", true);
        when(repository.organizationExists(organizationId)).thenReturn(true);
        when(repository.membership(userId, organizationId)).thenReturn(new DocumentRepository.Membership(
                "[\"TRAINER\"]", "[\"announcements.write\"]"));
    }

    @Test
    void rejectsFileWithMismatchedSignature() {
        MockMultipartFile file = new MockMultipartFile("file", "report.pdf", "application/pdf", "not a pdf".getBytes());

        assertThrows(OrganizationRequestException.class,
                () -> service.uploadAttachment(actor, organizationId, file));
        org.mockito.Mockito.verify(repository, never()).insertFile(any());
    }
}