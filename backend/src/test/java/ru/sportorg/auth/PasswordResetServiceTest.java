package ru.sportorg.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

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
    private SessionRegistry sessionRegistry;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(accountRepository, tokenRepository, rateLimitRepository,
                passwordEncoder, sessionRegistry, eventPublisher, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void unknownEmailDoesNotPublishAnythingOrExposeAccountExistence() {
        when(rateLimitRepository.claim(eq("PASSWORD_RESET"), anyString(), eq(NOW), any())).thenReturn(true);
        when(accountRepository.findForAuthentication("unknown@example.org")).thenReturn(Optional.empty());

        passwordResetService.requestReset(new PasswordResetRequest("unknown@example.org"), "192.0.2.20");

        verify(eventPublisher, never()).publishEvent(any());
        verify(tokenRepository, never()).create(any(), anyString(), anyString(), any(), any());
    }

    @Test
    void resetCreatesShortLivedHashedTokenForActiveVerifiedAccount() throws Exception {
        UUID userId = UUID.randomUUID();
        var account = new AuthenticatedUser(userId, "person@example.org", "person@example.org", "Person",
                null, "hash", "USER", "ACTIVE", true);
        when(rateLimitRepository.claim(eq("PASSWORD_RESET"), anyString(), eq(NOW), any())).thenReturn(true);
        when(accountRepository.findForAuthentication("person@example.org")).thenReturn(Optional.of(account));

        passwordResetService.requestReset(new PasswordResetRequest("person@example.org"), "192.0.2.20");

        var hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(tokenRepository).create(eq(userId), eq("PASSWORD_RESET"), hashCaptor.capture(),
                eq(NOW.plusSeconds(1800)), eq(NOW));
        var eventCaptor = ArgumentCaptor.forClass(PasswordResetEmailRequested.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(sha256(eventCaptor.getValue().token()), hashCaptor.getValue());
    }

    @Test
    void successfulResetConsumesTokensAndExpiresSessions() throws Exception {
        UUID userId = UUID.randomUUID();
        var session = org.mockito.Mockito.mock(SessionInformation.class);
        when(tokenRepository.findValidTokenForUpdate(anyString(), eq("PASSWORD_RESET"), eq(NOW)))
                .thenReturn(Optional.of(userId));
        when(passwordEncoder.encode("new-long-password-value" )).thenReturn("new-hash");
        when(accountRepository.updatePassword(userId, "new-hash", NOW)).thenReturn(true);
        when(sessionRegistry.getAllSessions(any(AuthenticatedUser.class), eq(false))).thenReturn(List.of(session));

        passwordResetService.confirmReset(new PasswordResetConfirmRequest("reset-token", "new-long-password-value"));

        var tokenHashCaptor = ArgumentCaptor.forClass(String.class);
        verify(tokenRepository).findValidTokenForUpdate(tokenHashCaptor.capture(), eq("PASSWORD_RESET"), eq(NOW));
        assertEquals(sha256("reset-token"), tokenHashCaptor.getValue());
        verify(tokenRepository).markUsed(tokenHashCaptor.getValue(), NOW);
        verify(tokenRepository).invalidateTokens(userId, "PASSWORD_RESET", NOW);
        verify(session).expireNow();
    }

    @Test
    void changeRejectsWrongCurrentPasswordWithoutUpdatingAccount() {
        var user = new AuthenticatedUser(UUID.randomUUID(), "person@example.org", "person@example.org",
                "Person", null, "old-hash", "USER", "ACTIVE", true);
        when(passwordEncoder.matches("wrong-current", "old-hash")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> passwordResetService.changePassword(user,
                new ChangePasswordRequest("wrong-current", "new-long-password-value")));

        verify(accountRepository, never()).updatePassword(any(), anyString(), any());
    }

    private static String sha256(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }
}
