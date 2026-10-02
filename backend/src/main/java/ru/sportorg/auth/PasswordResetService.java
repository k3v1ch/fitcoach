package ru.sportorg.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class PasswordResetService {

    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(30);
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);

    private final AccountRepository accountRepository;
    private final AuthTokenRepository tokenRepository;
    private final AuthRateLimitRepository rateLimitRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionRegistry sessionRegistry;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    PasswordResetService(AccountRepository accountRepository, AuthTokenRepository tokenRepository,
                         AuthRateLimitRepository rateLimitRepository, PasswordEncoder passwordEncoder,
                         SessionRegistry sessionRegistry, ApplicationEventPublisher eventPublisher, Clock clock) {
        this.accountRepository = accountRepository;
        this.tokenRepository = tokenRepository;
        this.rateLimitRepository = rateLimitRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionRegistry = sessionRegistry;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    void requestReset(PasswordResetRequest request, String remoteAddress) {
        Instant now = clock.instant();
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        boolean emailAllowed = rateLimitRepository.claim("PASSWORD_RESET", hash("email:" + normalizedEmail),
                now, now.minus(RATE_LIMIT_WINDOW));
        boolean ipAllowed = rateLimitRepository.claim("PASSWORD_RESET", hash("ip:" + remoteAddress),
                now, now.minus(RATE_LIMIT_WINDOW));
        if (!emailAllowed || !ipAllowed) {
            return;
        }

        var account = accountRepository.findForAuthentication(normalizedEmail);
        if (account.isEmpty() || !"ACTIVE".equals(account.get().status()) || !account.get().emailVerified()) {
            return;
        }

        String token = createRawToken();
        tokenRepository.invalidateTokens(account.get().userId(), "PASSWORD_RESET", now);
        tokenRepository.create(account.get().userId(), "PASSWORD_RESET", hash(token), now.plus(TOKEN_LIFETIME), now);
        eventPublisher.publishEvent(new PasswordResetEmailRequested(account.get().email(), token));
    }

    @Transactional
    void confirmReset(PasswordResetConfirmRequest request) {
        Instant now = clock.instant();
        String tokenHash = hash(request.token());
        var userId = tokenRepository.findValidTokenForUpdate(tokenHash, "PASSWORD_RESET", now)
                .orElseThrow(InvalidRegistrationTokenException::new);
        if (!accountRepository.updatePassword(userId, passwordEncoder.encode(request.newPassword()), now)) {
            throw new InvalidRegistrationTokenException();
        }
        tokenRepository.markUsed(tokenHash, now);
        tokenRepository.invalidateTokens(userId, "PASSWORD_RESET", now);
        expireSessions(userId);
    }

    @Transactional
    void changePassword(AuthenticatedUser user, ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }
        Instant now = clock.instant();
        if (!accountRepository.updatePassword(user.userId(), passwordEncoder.encode(request.newPassword()), now)) {
            throw new InvalidCredentialsException();
        }
        expireSessions(user.userId());
    }

    private void expireSessions(java.util.UUID userId) {
        AuthenticatedUser principal = new AuthenticatedUser(userId, "", "", "", null,
                "", "USER", "ACTIVE", true);
        for (SessionInformation session : sessionRegistry.getAllSessions(principal, false)) {
            session.expireNow();
        }
    }

    private String createRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}