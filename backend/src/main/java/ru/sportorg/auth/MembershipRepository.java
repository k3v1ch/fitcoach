package ru.sportorg.auth;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class MembershipRepository {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    MembershipRepository(JdbcClient jdbcClient, ObjectMapper objectMapper) {
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
    }

    List<OrganizationAccess> findActiveAccesses(UUID userId) {
        return jdbcClient.sql("""
                        SELECT organization_id, organization.name AS organization_name,
                               roles::text AS roles, permissions::text AS permissions
                        FROM membership
                        JOIN organization ON organization.id = membership.organization_id
                        WHERE user_id = :userId AND membership.status = 'ACTIVE'
                        ORDER BY organization.name, organization.id
                        """)
                .param("userId", userId)
                .query((resultSet, rowNumber) -> new OrganizationAccess(
                        resultSet.getObject("organization_id", UUID.class),
                        resultSet.getString("organization_name"),
                        readStringList(resultSet.getString("roles")),
                        readStringList(resultSet.getString("permissions"))))
                .list();
    }

    private List<String> readStringList(String json) {
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (IOException exception) {
            throw new IllegalStateException("Stored membership data is invalid", exception);
        }
    }
}