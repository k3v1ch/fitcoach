package ru.sportorg.auth;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-01T00:00:00Z");

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private SessionAuthenticationStrategy sessionAuthenticationStrategy;

    @Mock
    private SecurityContextRepository securityContextRepository;

    @Mock
    private CsrfTokenRepository csrfTokenRepository;

    @Mock
    private AccountRepository accountRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(authenticationManager, sessionAuthenticationStrategy,
                securityContextRepository, csrfTokenRepository, accountRepository,
                Clock.fixed(NOW, ZoneOffset.UTC), false);
    }

    @Test
    void loginRotatesSessionSecurityContextAndCsrfToken() {
        UUID userId = UUID.randomUUID();
        var user = new AuthenticatedUser(userId, "person@example.org", "person@example.org", "Person",
                null, "password-hash", "USER", "ACTIVE", true);
        Authentication authenticated = UsernamePasswordAuthenticationToken.authenticated(
                user, "", user.getAuthorities());
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticated);
        when(csrfTokenRepository.generateToken(any(HttpServletRequest.class)))
                .thenReturn(new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "fresh-token"));
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        CurrentUser currentUser = authService.login(new LoginRequest(" Person@Example.org ", "password"),
                request, response);

        HttpSession session = request.getSession(false);
        assertNotNull(session);
        assertEquals(NOW.plusMillis(SessionExpiry.LIFETIME_MILLIS).toEpochMilli(),
                session.getAttribute(SessionExpiry.ATTRIBUTE));
        assertEquals(userId, currentUser.userId());
        assertEquals(NOW.plusMillis(SessionExpiry.LIFETIME_MILLIS), currentUser.expiresAt());
        assertEquals("fresh-token", response.getHeader("X-CSRF-TOKEN"));
        assertFalse(currentUser.toString().contains("password-hash"));
        verify(sessionAuthenticationStrategy).onAuthentication(authenticated, request, response);
        verify(securityContextRepository).saveContext(any(), org.mockito.ArgumentMatchers.eq(request),
                org.mockito.ArgumentMatchers.eq(response));
        verify(accountRepository).updateLastLogin(userId, NOW);
    }

    @Test
        void meReturnsCurrentAccountWithoutOrganizationData() {
        UUID userId = UUID.randomUUID();
        var user = new AuthenticatedUser(userId, "person@example.org", "person@example.org", "Person",
                null, "password-hash", "USER", "ACTIVE", true);
        var session = new MockHttpServletRequest().getSession(true);
        session.setAttribute(SessionExpiry.ATTRIBUTE, NOW.plusSeconds(3600).toEpochMilli());

        CurrentUser currentUser = authService.currentUser(user, session);

                assertEquals(userId, currentUser.userId());
                assertEquals("person@example.org", currentUser.email());
                var fields = Arrays.stream(CurrentUser.class.getRecordComponents())
                        .map(component -> component.getName())
                        .toList();
                assertFalse(fields.contains("organizations"));
                assertFalse(fields.contains("sections"));
        assertEquals(NOW.plusSeconds(3600), currentUser.expiresAt());
    }
}
