package ru.sportorg.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class AuthTokenRepository {

    private final JdbcClient jdbcClient;

    AuthTokenRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    void invalidateRegistrationTokens(UUID userId, Instant now) {
        jdbcClient.sql("""
                        UPDATE auth_token
                        SET used_at = :now
                        WHERE user_id = :userId AND purpose = 'REGISTRATION' AND used_at IS NULL
                        """)
                .param("now", now)
                .param("userId", userId)
                .update();
    }

    void create(UUID userId, String purpose, String tokenHash, Instant expiresAt, Instant now) {
        jdbcClient.sql("""
                        INSERT INTO auth_token (user_id, purpose, token_hash, expires_at, created_at)
                        VALUES (:userId, :purpose, :tokenHash, :expiresAt, :now)
                        """)
                .param("userId", userId)
                .param("purpose", purpose)
                .param("tokenHash", tokenHash)
                .param("expiresAt", expiresAt)
                .param("now", now)
                .update();
    }

    Optional<UUID> findValidRegistrationTokenForUpdate(String tokenHash, Instant now) {
        return jdbcClient.sql("""
                        SELECT user_id
                        FROM auth_token
                        WHERE token_hash = :tokenHash
                          AND purpose = 'REGISTRATION'
                          AND used_at IS NULL
                          AND expires_at > :now
                        FOR UPDATE
                        """)
                .param("tokenHash", tokenHash)
                .param("now", now)
                .query(UUID.class)
                .optional();
    }

        Optional<UUID> findValidTokenForUpdate(String tokenHash, String purpose, Instant now) {
                return jdbcClient.sql("""
                                                SELECT user_id
                                                FROM auth_token
                                                WHERE token_hash = :tokenHash
                                                    AND purpose = :purpose
                                                    AND used_at IS NULL
                                                    AND expires_at > :now
                                                FOR UPDATE
                                                """)
                                .param("tokenHash", tokenHash)
                                .param("purpose", purpose)
                                .param("now", now)
                                .query(UUID.class)
                                .optional();
        }

        void invalidateTokens(UUID userId, String purpose, Instant now) {
                jdbcClient.sql("""
                                                UPDATE auth_token SET used_at = :now
                                                WHERE user_id = :userId AND purpose = :purpose AND used_at IS NULL
                                                """)
                                .param("now", now)
                                .param("userId", userId)
                                .param("purpose", purpose)
                                .update();
        }

    void markUsed(String tokenHash, Instant now) {
        jdbcClient.sql("UPDATE auth_token SET used_at = :now WHERE token_hash = :tokenHash AND used_at IS NULL")
                .param("now", now)
                .param("tokenHash", tokenHash)
                .update();
    }
}