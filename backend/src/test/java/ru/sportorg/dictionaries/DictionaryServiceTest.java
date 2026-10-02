package ru.sportorg.dictionaries;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
class DictionaryServiceTest {
    @Mock private DictionaryRepository repository;
    private DictionaryService service;
    private UUID organizationId;
    private UUID userId;
    private AuthenticatedUser actor;

    @BeforeEach
    void setUp() {
        service = new DictionaryService(repository, Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC));
        organizationId = UUID.randomUUID(); userId = UUID.randomUUID();
        actor = new AuthenticatedUser(userId, "parent@example.org", "parent@example.org", "Parent", null, "hash", "USER", "ACTIVE", true);
        when(repository.organizationExists(organizationId)).thenReturn(true);
    }

    @Test
    void parentCannotEditDictionary() {
        when(repository.membership(userId, organizationId)).thenReturn(new DictionaryRepository.Membership("[\"PARENT\"]", "[\"sections.read\"]"));
        assertThrows(OrganizationPermissionException.class, () -> service.create(actor, organizationId, "sport-types", new DictionaryWrite("Running", null, null, 0, "ACTIVE")));
        verify(repository, never()).insert(any(), any(), any());
    }

    @Test
    void addressIsAllowedOnlyForVenues() {
        when(repository.membership(userId, organizationId)).thenReturn(new DictionaryRepository.Membership("[\"TRAINER\"]", "[\"dictionaries.write\"]"));
        assertThrows(OrganizationRequestException.class, () -> service.create(actor, organizationId, "sport-types", new DictionaryWrite("Running", null, "Address", 0, "ACTIVE")));
        verify(repository, never()).insert(any(), any(), any());
    }
}