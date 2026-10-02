package ru.sportorg.organizations;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
                .param("updatedAt", updatedAt)
                .param("organizationId", organizationId)
                .update();
    }

    long countMembers(UUID organizationId, String q, String role, String status) {
        return jdbcClient.sql("""
                        SELECT count(*)
                        FROM membership m JOIN app_user u ON u.id = m.user_id
                        WHERE m.organization_id = :organizationId
                          AND (:q IS NULL OR position(:q IN lower(u.full_name)) > 0)
                          AND (:role IS NULL OR m.roles @> jsonb_build_array(:role))
                          AND (:status IS NULL OR m.status = :status)
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
                          AND (:q IS NULL OR position(:q IN lower(u.full_name)) > 0)
                          AND (:role IS NULL OR m.roles @> jsonb_build_array(:role))
                          AND (:status IS NULL OR m.status = :status)
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

    private List<String> readStringList(String json) {
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (IOException exception) {
            throw new IllegalStateException("Stored membership data is invalid", exception);
        }
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    record MembershipAccess(List<String> roles, List<String> permissions) {
    }
}