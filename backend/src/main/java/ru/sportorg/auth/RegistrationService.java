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
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RegistrationService {

    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(30);
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);

    private final AccountRepository accountRepository;
    private final AuthTokenRepository tokenRepository;
    private final AuthRateLimitRepository rateLimitRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    RegistrationService(AccountRepository accountRepository, AuthTokenRepository tokenRepository,
                        AuthRateLimitRepository rateLimitRepository, PasswordEncoder passwordEncoder,
                        ApplicationEventPublisher eventPublisher, Clock clock) {
        this.accountRepository = accountRepository;
        this.tokenRepository = tokenRepository;
        this.rateLimitRepository = rateLimitRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    void requestRegistration(RegistrationRequest request, String remoteAddress) {
        Instant now = clock.instant();
        String normalizedEmail = normalizeEmail(request.email());
        claimRateLimit("REGISTRATION", "email:" + normalizedEmail, now);
        claimRateLimit("REGISTRATION", "ip:" + remoteAddress, now);

        var existing = accountRepository.findByNormalizedEmailForUpdate(normalizedEmail);
        if (existing.isPresent() && !existing.get().status().equals("PENDING_EMAIL")) {
            return;
        }

        UUID userId;
        if (existing.isPresent()) {
            userId = existing.get().id();
            tokenRepository.invalidateRegistrationTokens(userId, now);
        } else {
            var created = accountRepository.createPending(
                    request.email().trim(), normalizedEmail,
                    "",
                    request.accountType(), now);
            if (created.isEmpty()) {
                var concurrentAccount = accountRepository.findByNormalizedEmailForUpdate(normalizedEmail);
                if (concurrentAccount.isEmpty() || !concurrentAccount.get().status().equals("PENDING_EMAIL")) {
                    return;
                }
                userId = concurrentAccount.get().id();
                tokenRepository.invalidateRegistrationTokens(userId, now);
            } else {
                userId = created.get();
            }
        }

        String token = createRawToken();
        tokenRepository.create(userId, "REGISTRATION", hash(token), now.plus(TOKEN_LIFETIME), now);
        String email = existing.map(AccountRepository.AccountRecord::email).orElse(request.email().trim());
        eventPublisher.publishEvent(new RegistrationEmailRequested(email, token));
    }

    @Transactional
    void confirmRegistration(ConfirmRegistrationRequest request) {
        Instant now = clock.instant();
        String tokenHash = hash(request.getToken());
        UUID userId = tokenRepository.findValidRegistrationTokenForUpdate(tokenHash, now)
                .orElseThrow(InvalidRegistrationTokenException::new);
        String passwordHash = passwordEncoder.encode(request.getPassword());
        boolean activated = accountRepository.activate(userId, passwordHash,
                request.getFullName(), request.isFullNameProvided(),
                request.getAccountType(), now);
        if (!activated) {
            throw new InvalidRegistrationTokenException();
        }
        tokenRepository.markUsed(tokenHash, now);
    }

    private void claimRateLimit(String purpose, String subject, Instant now) {
        if (!rateLimitRepository.claim(purpose, hash(subject), now, now.minus(RATE_LIMIT_WINDOW))) {
            throw new RegistrationRateLimitException();
        }
    }

    private String createRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
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