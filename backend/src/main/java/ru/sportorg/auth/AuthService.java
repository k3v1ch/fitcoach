package ru.sportorg.auth;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.stereotype.Service;

@Service
class AuthService {

    private final AuthenticationManager authenticationManager;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextRepository securityContextRepository;
    private final CsrfTokenRepository csrfTokenRepository;
    private final AccountRepository accountRepository;
    private final MembershipRepository membershipRepository;
    private final Clock clock;
    private final boolean secureCookie;

    AuthService(AuthenticationManager authenticationManager,
                 SessionAuthenticationStrategy sessionAuthenticationStrategy,
                 SecurityContextRepository securityContextRepository,
                 CsrfTokenRepository csrfTokenRepository,
                 AccountRepository accountRepository,
                 MembershipRepository membershipRepository,
                 Clock clock,
                 @org.springframework.beans.factory.annotation.Value("${server.servlet.session.cookie.secure:true}")
                 boolean secureCookie) {
        this.authenticationManager = authenticationManager;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.securityContextRepository = securityContextRepository;
        this.csrfTokenRepository = csrfTokenRepository;
        this.accountRepository = accountRepository;
        this.membershipRepository = membershipRepository;
        this.clock = clock;
        this.secureCookie = secureCookie;
    }

    CurrentUser login(LoginRequest request, HttpServletRequest servletRequest, HttpServletResponse response) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.email().trim().toLowerCase(Locale.ROOT), request.password()));
        } catch (AuthenticationException exception) {
            throw new InvalidCredentialsException();
        }

        servletRequest.getSession(true);
        sessionAuthenticationStrategy.onAuthentication(authentication, servletRequest, response);

        Instant expiresAt = clock.instant().plusMillis(SessionExpiry.LIFETIME_MILLIS);
        HttpSession session = servletRequest.getSession(true);
        session.setAttribute(SessionExpiry.ATTRIBUTE, expiresAt.toEpochMilli());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, response);

        CsrfToken csrfToken = csrfTokenRepository.generateToken(servletRequest);
        csrfTokenRepository.saveToken(csrfToken, servletRequest, response);
        response.setHeader(csrfToken.getHeaderName(), csrfToken.getToken());

        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        accountRepository.updateLastLogin(user.userId(), clock.instant());
        return currentUser(user, expiresAt);
    }

    CurrentUser currentUser(AuthenticatedUser user, HttpSession session) {
        Object expiresAt = session.getAttribute(SessionExpiry.ATTRIBUTE);
        if (!(expiresAt instanceof Long expiry)) {
            throw new InvalidCredentialsException();
        }
        return currentUser(user, Instant.ofEpochMilli(expiry));
    }

    void logout(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        ResponseCookie clearedCookie = ResponseCookie.from("JSESSIONID", "")
                .path("/")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, clearedCookie.toString());
    }

    private CurrentUser currentUser(AuthenticatedUser user, Instant expiresAt) {
        return new CurrentUser(user.userId(), user.email(), user.fullName(), user.appRole(),
                expiresAt, membershipRepository.findActiveAccesses(user.userId()));
    }
}