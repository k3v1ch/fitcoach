package ru.sportorg.auth;

import java.io.IOException;
import java.time.Clock;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AbsoluteSessionExpiryFilter extends OncePerRequestFilter {

    private final Clock clock;
    private final SessionRegistry sessionRegistry;

    AbsoluteSessionExpiryFilter(Clock clock, SessionRegistry sessionRegistry) {
        this.clock = clock;
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        var session = request.getSession(false);
        if (session != null) {
            Object expiry = session.getAttribute(SessionExpiry.ATTRIBUTE);
                var registeredSession = sessionRegistry.getSessionInformation(session.getId());
                if ((expiry instanceof Long expiresAt && clock.millis() >= expiresAt)
                    || (registeredSession != null && registeredSession.isExpired())) {
                session.invalidate();
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}