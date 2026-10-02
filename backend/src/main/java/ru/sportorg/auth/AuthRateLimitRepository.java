package ru.sportorg.auth;

import java.time.Instant;
import java.time.ZoneOffset;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class AuthRateLimitRepository {

    private final JdbcClient jdbcClient;

    AuthRateLimitRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    boolean claim(String purpose, String keyHash, Instant now, Instant allowedAfter) {
        return jdbcClient.sql("""
                        INSERT INTO auth_rate_limit (purpose, request_key_hash, requested_at)
                        VALUES (:purpose, :keyHash, :now)
                        ON CONFLICT (purpose, request_key_hash) DO UPDATE
                        SET requested_at = EXCLUDED.requested_at
                        WHERE auth_rate_limit.requested_at <= :allowedAfter
                        RETURNING request_key_hash
                        """)
                .param("purpose", purpose)
                .param("keyHash", keyHash)
                .param("now", now.atOffset(ZoneOffset.UTC))
                .param("allowedAfter", allowedAfter.atOffset(ZoneOffset.UTC))
                .query(String.class)
                .optional()
                .isPresent();
    }
}