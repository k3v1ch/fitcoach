package ru.sportorg.events;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
class EventServiceTest {
    @Mock private EventRepository repository;
    private EventService service;
    private UUID organizationId;
    private UUID userId;
    private AuthenticatedUser actor;

    @BeforeEach
    void setUp() {
        service = new EventService(repository, new ObjectMapper(), Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC));
        organizationId = UUID.randomUUID(); userId = UUID.randomUUID();
        actor = new AuthenticatedUser(userId, "user@example.org", "user@example.org", "User", null, "hash", "USER", "ACTIVE", true);
        when(repository.organizationExists(organizationId)).thenReturn(true);
    }

    @Test
    void parentCannotCreateEvent() {
        when(repository.membership(userId, organizationId)).thenReturn(Optional.of(
                new EventRepository.MembershipAccess(List.of("PARENT"), List.of("events.write"))));

        assertThrows(OrganizationPermissionException.class, () -> service.create(actor, organizationId,
                new EventWrite("Camp", "CAMP", UUID.randomUUID(), null, null, null, null, null,
                        null, null, null, List.of())));
        verify(repository, never()).insert(any(), any(), any(), any(), any());
    }

    @Test
    void fundraiserRequiresPositiveTargetAndDueDate() {
        when(repository.membership(userId, organizationId)).thenReturn(Optional.of(
                new EventRepository.MembershipAccess(List.of("TRAINER"), List.of("events.write"))));
        when(repository.sectionExists(eq(organizationId), any())).thenReturn(true);
        EventWrite fundraiser = new EventWrite("Fund", "FUNDRAISER", UUID.randomUUID(), null, null, null, null,
                BigDecimal.ZERO, null, null, null, List.of());

        assertThrows(OrganizationRequestException.class, () -> service.create(actor, organizationId, fundraiser));
        verify(repository, never()).insert(any(), any(), any(), any(), any());
    }
}