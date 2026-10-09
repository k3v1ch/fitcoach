package ru.sportorg.organizations;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ru.sportorg.jdbc.JdbcTime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class OrganizationRepository {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    OrganizationRepository(JdbcClient jdbcClient, ObjectMapper objectMapper) {
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
    }

    Optional<Organization> findOrganization(UUID organizationId) {
        return jdbcClient.sql("""
                        SELECT id, name, description, address, timezone, created_at, updated_at
                        FROM organization WHERE id = :organizationId
                        """)
                .param("organizationId", organizationId)
                .query((resultSet, rowNumber) -> new Organization(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("name"),
                        resultSet.getString("description"),
                        resultSet.getString("address"),
                        resultSet.getString("timezone"),
                        instant(resultSet.getTimestamp("created_at")),
                        instant(resultSet.getTimestamp("updated_at"))))
                .optional();
    }

    List<OrganizationAccess> findOrganizationsForUser(UUID userId) {
        return jdbcClient.sql("""
                        SELECT o.id, o.name, m.roles::text AS roles, m.permissions::text AS permissions
                        FROM membership m
                        JOIN organization o ON o.id = m.organization_id
                        WHERE m.user_id = :userId AND m.status = 'ACTIVE'
                        ORDER BY o.name, o.id
                        """)
                .param("userId", userId)
                .query((resultSet, rowNumber) -> new OrganizationAccess(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("name"),
                        readStringList(resultSet.getString("roles")),
                        readStringList(resultSet.getString("permissions"))))
                .list();
    }

    Organization createOrganization(UUID creatorUserId, String name, String description, String address,
                                    String timezone, List<String> permissions, Instant now) {
        UUID organizationId = jdbcClient.sql("""
                        INSERT INTO organization (name, description, address, timezone, created_at, updated_at)
                        VALUES (:name, :description, :address, :timezone, :now, :now)
                        RETURNING id
                        """)
                .param("name", name)
                .param("description", description)
                .param("address", address)
                .param("timezone", timezone)
                .param("now", JdbcTime.toOffsetDateTime(now))
                .query((resultSet, rowNumber) -> resultSet.getObject("id", UUID.class))
                .single();

        jdbcClient.sql("""
                        INSERT INTO membership (user_id, organization_id, roles, permissions, status,
                                                created_at, updated_at)
                        VALUES (:userId, :organizationId, '["TRAINER"]'::jsonb, CAST(:permissions AS jsonb),
                                'ACTIVE', :now, :now)
                        """)
                .param("userId", creatorUserId)
                .param("organizationId", organizationId)
                .param("permissions", writeStringList(permissions))
                .param("now", JdbcTime.toOffsetDateTime(now))
                .update();

        return findOrganization(organizationId).orElseThrow();
    }

    Optional<MembershipAccess> findActiveMembership(UUID userId, UUID organizationId) {
        return jdbcClient.sql("""
                        SELECT roles::text AS roles, permissions::text AS permissions
                        FROM membership
                        WHERE user_id = :userId AND organization_id = :organizationId AND status = 'ACTIVE'
                        """)
                .param("userId", userId)
                .param("organizationId", organizationId)
                .query((resultSet, rowNumber) -> new MembershipAccess(
                        readStringList(resultSet.getString("roles")),
                        readStringList(resultSet.getString("permissions"))))
                .optional();
    }

    void patchOrganization(UUID organizationId, OrganizationPatch patch, Instant updatedAt) {
        jdbcClient.sql("""
                        UPDATE organization
                        SET name = CASE WHEN :nameProvided THEN :name ELSE name END,
                            description = CASE WHEN :descriptionProvided THEN :description ELSE description END,
                            address = CASE WHEN :addressProvided THEN :address ELSE address END,
                            timezone = CASE WHEN :timezoneProvided THEN :timezone ELSE timezone END,
                            updated_at = :updatedAt
                        WHERE id = :organizationId
                        """)
                .param("nameProvided", patch.isNameProvided())
                .param("name", patch.getName())
                .param("descriptionProvided", patch.isDescriptionProvided())
                .param("description", patch.getDescription())
                .param("addressProvided", patch.isAddressProvided())
                .param("address", patch.getAddress())
                .param("timezoneProvided", patch.isTimezoneProvided())
                .param("timezone", patch.getTimezone())
                .param("updatedAt", JdbcTime.toOffsetDateTime(updatedAt))
                .param("organizationId", organizationId)
                .update();
    }

    long countMembers(UUID organizationId, String q, String role, String status) {
        return jdbcClient.sql("""
                        SELECT count(*)
                        FROM membership m JOIN app_user u ON u.id = m.user_id
                        WHERE m.organization_id = :organizationId
                          AND (CAST(:q AS text) IS NULL OR position(:q IN lower(u.full_name)) > 0)
                          AND (CAST(:role AS text) IS NULL OR m.roles @> jsonb_build_array(CAST(:role AS text)))
                          AND (CAST(:status AS text) IS NULL OR m.status = :status)
                        """)
                .param("organizationId", organizationId)
                .param("q", q)
                .param("role", role)
                .param("status", status)
                .query(Long.class)
                .single();
    }

    List<OrganizationMember> findMembers(UUID organizationId, String q, String role, String status,
                                        int limit, int offset) {
        return jdbcClient.sql("""
                        SELECT u.id AS user_id, u.full_name, m.roles::text AS roles, m.status
                        FROM membership m JOIN app_user u ON u.id = m.user_id
                        WHERE m.organization_id = :organizationId
                          AND (CAST(:q AS text) IS NULL OR position(:q IN lower(u.full_name)) > 0)
                          AND (CAST(:role AS text) IS NULL OR m.roles @> jsonb_build_array(CAST(:role AS text)))
                          AND (CAST(:status AS text) IS NULL OR m.status = :status)
                        ORDER BY u.created_at DESC, u.id DESC
                        LIMIT :limit OFFSET :offset
                        """)
                .param("organizationId", organizationId)
                .param("q", q)
                .param("role", role)
                .param("status", status)
                .param("limit", limit)
                .param("offset", offset)
                .query((resultSet, rowNumber) -> new OrganizationMember(
                        resultSet.getObject("user_id", UUID.class),
                        resultSet.getString("full_name"),
                        readStringList(resultSet.getString("roles")),
                        resultSet.getString("status")))
                .list();
    }

    Optional<OrganizationMember> addParentMembership(UUID organizationId, String normalizedEmail,
                                                     List<String> permissions, Instant updatedAt) {
        return addSelfServiceMembership(organizationId, normalizedEmail, "PARENT", permissions, updatedAt);
    }

    Optional<OrganizationMember> addAthleteMembership(UUID organizationId, String normalizedEmail,
                                                      List<String> permissions, Instant updatedAt) {
        return addSelfServiceMembership(organizationId, normalizedEmail, "ATHLETE", permissions, updatedAt);
    }

    /** Членство по email подтверждённого аккаунта: роль добавляется к уже имеющимся, AGENCY не меняется. */
    private Optional<OrganizationMember> addSelfServiceMembership(UUID organizationId, String normalizedEmail, String role,
                                                                  List<String> permissions, Instant updatedAt) {
        String permissionsJson = writeStringList(permissions);
        return jdbcClient.sql("""
                        WITH upserted AS (
                            INSERT INTO membership (user_id, organization_id, roles, permissions, status,
                                                    created_at, updated_at)
                            SELECT u.id, :organizationId, jsonb_build_array(CAST(:role AS text)), CAST(:permissions AS jsonb),
                                   'ACTIVE', :updatedAt, :updatedAt
                            FROM app_user u
                            WHERE u.normalized_email = :normalizedEmail
                              AND u.status = 'ACTIVE'
                              AND u.email_verified_at IS NOT NULL
                            ON CONFLICT (user_id, organization_id) DO UPDATE
                            SET roles = (
                                    SELECT jsonb_agg(DISTINCT role ORDER BY role)
                                    FROM jsonb_array_elements_text(membership.roles || EXCLUDED.roles) AS roles(role)
                                ),
                                permissions = (
                                    SELECT jsonb_agg(DISTINCT permission ORDER BY permission)
                                    FROM jsonb_array_elements_text(membership.permissions || EXCLUDED.permissions) AS permissions(permission)
                                ),
                                updated_at = EXCLUDED.updated_at
                            WHERE membership.status = 'ACTIVE'
                              AND NOT (membership.roles @> '["AGENCY"]'::jsonb)
                            RETURNING user_id, roles, status
                        )
                        SELECT u.id AS user_id, u.full_name, upserted.roles::text AS roles, upserted.status
                        FROM upserted
                        JOIN app_user u ON u.id = upserted.user_id
                        """)
                .param("organizationId", organizationId)
                .param("normalizedEmail", normalizedEmail)
                .param("role", role)
                .param("permissions", permissionsJson)
                .param("updatedAt", JdbcTime.toOffsetDateTime(updatedAt))
                .query((resultSet, rowNumber) -> new OrganizationMember(
                        resultSet.getObject("user_id", UUID.class),
                        resultSet.getString("full_name"),
                        readStringList(resultSet.getString("roles")),
                        resultSet.getString("status")))
                .optional();
    }

    private List<String> readStringList(String json) {
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (IOException exception) {
            throw new IllegalStateException("Stored membership data is invalid", exception);
        }
    }

    private String writeStringList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (IOException exception) {
            throw new IllegalStateException("Membership data cannot be serialized.", exception);
        }
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    record MembershipAccess(List<String> roles, List<String> permissions) {
    }
}