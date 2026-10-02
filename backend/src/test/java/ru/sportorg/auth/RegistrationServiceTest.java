package ru.sportorg.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-01T00:00:00Z");

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AuthTokenRepository tokenRepository;

    @Mock
    private AuthRateLimitRepository rateLimitRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationService(
            accountRepository, tokenRepository, rateLimitRepository,
            passwordEncoder, eventPublisher, Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void registrationStoresOnlyTokenHashAndPublishesMailAfterTransaction() throws Exception {
        UUID userId = UUID.randomUUID();
        when(rateLimitRepository.claim(eq("REGISTRATION"), anyString(), eq(NOW), any())).thenReturn(true);
        when(accountRepository.findByNormalizedEmailForUpdate("person@example.org")).thenReturn(Optional.empty());
        when(accountRepository.createPending("Person@Example.org", "person@example.org", "",
            RegistrationAccountType.TRAINER, NOW)).thenReturn(Optional.of(userId));

        registrationService.requestRegistration(
            new RegistrationRequest(" Person@Example.org ", RegistrationAccountType.TRAINER), "192.0.2.10"
        );

        var tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(tokenRepository).create(eq(userId), eq("REGISTRATION"), tokenCaptor.capture(),
            eq(NOW.plusSeconds(1800)), eq(NOW));
        String tokenHash = tokenCaptor.getValue();
        assertEquals(64, tokenHash.length());

        var eventCaptor = ArgumentCaptor.forClass(RegistrationEmailRequested.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals("Person@Example.org", eventCaptor.getValue().email());
        assertNotEquals(tokenHash, eventCaptor.getValue().token());
        assertEquals(tokenHash, sha256(eventCaptor.getValue().token()));
    }

    @Test
    void confirmationActivatesAccountAndConsumesToken() throws Exception {
        UUID userId = UUID.randomUUID();
        var request = new ConfirmRegistrationRequest();
        request.setToken("activation-token");
        request.setPassword("a-long-enough-password");
        request.setFullName("Corrected Name");
        when(tokenRepository.findValidRegistrationTokenForUpdate(anyString(), eq(NOW)))
            .thenReturn(Optional.of(userId));
        request.setAccountType(RegistrationAccountType.ATHLETE);
        when(passwordEncoder.encode("a-long-enough-password")).thenReturn("argon-hash");
        when(accountRepository.activate(userId, "argon-hash", "Corrected Name", true,
            RegistrationAccountType.ATHLETE, NOW)).thenReturn(true);

        registrationService.confirmRegistration(request);

        var tokenHashCaptor = ArgumentCaptor.forClass(String.class);
        verify(tokenRepository).findValidRegistrationTokenForUpdate(tokenHashCaptor.capture(), eq(NOW));
        assertEquals(sha256("activation-token"), tokenHashCaptor.getValue());
        verify(tokenRepository).markUsed(tokenHashCaptor.getValue(), NOW);
    }

    @Test
    void confirmationRejectsMissingOrExpiredTokenWithoutChangingAccount() {
        var request = new ConfirmRegistrationRequest();
        request.setToken("expired-token");
        request.setPassword("a-long-enough-password");
        when(tokenRepository.findValidRegistrationTokenForUpdate(anyString(), eq(NOW)))
            .thenReturn(Optional.empty());

        assertThrows(InvalidRegistrationTokenException.class,
            () -> registrationService.confirmRegistration(request));

        verify(accountRepository, never()).activate(
            any(), anyString(), 
            any(), anyBoolean(),
            any(), any()
        );
        verify(passwordEncoder, never()).encode(any());
    }

    private static String sha256(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }
}