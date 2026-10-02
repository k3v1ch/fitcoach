package ru.sportorg.auth;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbsoluteSessionExpiryFilterTest {

    @Test
    void invalidatesSessionAtAbsoluteDeadline() throws Exception {
        var sessionRegistry = mock(SessionRegistry.class);
        var filter = new AbsoluteSessionExpiryFilter(
                Clock.fixed(Instant.parse("2026-10-01T08:00:00Z"), ZoneOffset.UTC), sessionRegistry);
        var request = new MockHttpServletRequest();
        var session = request.getSession(true);
        session.setAttribute(SessionExpiry.ATTRIBUTE,
                Instant.parse("2026-10-01T08:00:00Z").toEpochMilli());

        filter.doFilter(request, new MockHttpServletResponse(), (servletRequest, servletResponse) -> {
        });

        assertNull(request.getSession(false));
    }

    @Test
    void invalidatesSessionRevokedAfterPasswordChange() throws Exception {
        var sessionRegistry = mock(SessionRegistry.class);
        var request = new MockHttpServletRequest();
        var session = request.getSession(true);
        session.setAttribute(SessionExpiry.ATTRIBUTE,
                Instant.parse("2026-10-01T09:00:00Z").toEpochMilli());
        var sessionInformation = mock(SessionInformation.class);
        when(sessionRegistry.getSessionInformation(session.getId())).thenReturn(sessionInformation);
        when(sessionInformation.isExpired()).thenReturn(true);
        var filter = new AbsoluteSessionExpiryFilter(
                Clock.fixed(Instant.parse("2026-10-01T08:00:00Z"), ZoneOffset.UTC), sessionRegistry);

        filter.doFilter(request, new MockHttpServletResponse(), (servletRequest, servletResponse) -> {
        });

        assertNull(request.getSession(false));
    }
}
