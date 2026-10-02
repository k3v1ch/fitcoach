package ru.sportorg.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import ru.sportorg.jdbc.JdbcTime;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class AccountRepository {

    private final JdbcClient jdbcClient;

    AccountRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    Optional<AccountRecord> findByNormalizedEmailForUpdate(String normalizedEmail) {
        return jdbcClient.sql("""
                        SELECT id, email, status
                        FROM app_user
                        WHERE normalized_email = :normalizedEmail
                        FOR UPDATE
                        """)
                .param("normalizedEmail", normalizedEmail)
                .query((resultSet, rowNumber) -> new AccountRecord(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("email"),
                        resultSet.getString("status")))
                .optional();
    }

    Optional<UUID> createPending(String email, String normalizedEmail, String fullName,
                                RegistrationAccountType accountType, Instant now) {
        return jdbcClient.sql("""
                        INSERT INTO app_user (email, normalized_email, full_name, account_type, status, created_at, updated_at)
                        VALUES (:email, :normalizedEmail, :fullName, :accountType, 'PENDING_EMAIL', :now, :now)
                        ON CONFLICT (normalized_email) DO NOTHING
                        RETURNING id
                        """)
                .param("email", email)
                .param("normalizedEmail", normalizedEmail)
                .param("fullName", fullName == null ? "" : fullName)
                .param("accountType", accountType == null ? RegistrationAccountType.ATHLETE.name() : accountType.name())
                .param("now", JdbcTime.toOffsetDateTime(now))
                .query(UUID.class)
                .optional();
    }

    boolean activate(UUID userId, String passwordHash, String fullName, boolean fullNameProvided,
                     RegistrationAccountType accountType, Instant now) {
        return jdbcClient.sql("""
                        UPDATE app_user
                        SET status = 'ACTIVE',
                            password_hash = :passwordHash,
                            email_verified_at = :now,
                            full_name = CASE WHEN :fullNameProvided THEN :fullName ELSE full_name END,
                            account_type = :accountType,
                            updated_at = :now
                        WHERE id = :userId AND status = 'PENDING_EMAIL'
                        """)
                .param("passwordHash", passwordHash)
                .param("now", JdbcTime.toOffsetDateTime(now))
                .param("fullNameProvided", fullNameProvided)
                .param("fullName", fullName)
                .param("accountType", accountType == null ? RegistrationAccountType.ATHLETE.name() : accountType.name())
                .param("userId", userId)
                .update() == 1;
    }

            Optional<AuthenticatedUser> findForAuthentication(String normalizedEmail) {
            return jdbcClient.sql("""
                    SELECT id, email, normalized_email, full_name, password_hash, app_role, status,
                           email_verified_at
                    FROM app_user
                    WHERE normalized_email = :normalizedEmail
                    """)
                .param("normalizedEmail", normalizedEmail)
                .query((resultSet, rowNumber) -> new AuthenticatedUser(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getString("email"),
                    resultSet.getString("normalized_email"),
                    resultSet.getString("full_name"),
                    resultSet.getString("password_hash"),
                    resultSet.getString("app_role"),
                    resultSet.getString("status"),
                    resultSet.getTimestamp("email_verified_at") != null))
                .optional();
            }

            void updateLastLogin(UUID userId, Instant loggedInAt) {
            jdbcClient.sql("UPDATE app_user SET last_login_at = :loggedInAt WHERE id = :userId")
                .param("loggedInAt", JdbcTime.toOffsetDateTime(loggedInAt))
                .param("userId", userId)
                .update();
            }

            boolean updatePassword(UUID userId, String passwordHash, Instant updatedAt) {
                return jdbcClient.sql("""
                                UPDATE app_user
                                SET password_hash = :passwordHash, updated_at = :updatedAt
                                WHERE id = :userId AND status = 'ACTIVE' AND email_verified_at IS NOT NULL
                                """)
                        .param("passwordHash", passwordHash)
                        .param("updatedAt", JdbcTime.toOffsetDateTime(updatedAt))
                        .param("userId", userId)
                        .update() == 1;
            }

    record AccountRecord(UUID id, String email, String status) {
    }
}