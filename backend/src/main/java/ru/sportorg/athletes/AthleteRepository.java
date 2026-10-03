package ru.sportorg.athletes;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ru.sportorg.jdbc.JdbcTime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class AthleteRepository {

    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    AthleteRepository(JdbcClient jdbcClient, ObjectMapper objectMapper) {
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
    }

    Optional<MembershipAccess> findActiveMembership(UUID userId, UUID organizationId) {
        return jdbcClient.sql("""
                        SELECT roles::text AS roles, permissions::text AS permissions
                        FROM membership
                        WHERE user_id = :userId AND organization_id = :organizationId AND status = 'ACTIVE'
                        """)
                .param("userId", userId).param("organizationId", organizationId)
                .query((rs, row) -> new MembershipAccess(readList(rs.getString("roles")), readList(rs.getString("permissions"))))
                .optional();
    }

    boolean organizationExists(UUID organizationId) {
        return jdbcClient.sql("SELECT count(*) FROM organization WHERE id = :organizationId")
                .param("organizationId", organizationId).query(Long.class).single() > 0;
    }

    long count(UUID organizationId, String q, String status, UUID userId, String scope) {
        return jdbcClient.sql("""
                        SELECT count(*) FROM athlete a
                        WHERE a.organization_id = :organizationId
                          AND (CAST(:q AS text) IS NULL OR position(:q IN lower(concat_ws(' ', a.first_name, a.last_name, a.middle_name))) > 0)
                          AND (CAST(:status AS text) IS NULL OR a.status = :status)
                          AND (CAST(:userId AS uuid) IS NULL OR (:scope IN ('PARENT', 'BOTH') AND EXISTS (
                              SELECT 1 FROM parent_link pl WHERE pl.athlete_id = a.id AND pl.parent_user_id = :userId))
                              OR (:scope IN ('ATHLETE', 'BOTH') AND a.user_id = :userId))
                        """)
                .param("organizationId", organizationId).param("q", q).param("status", status)
                .param("userId", userId).param("scope", scope).query(Long.class).single();
    }

    List<Athlete> find(UUID organizationId, String q, String status, UUID userId, String scope,
                       int limit, int offset) {
        return jdbcClient.sql("""
                        SELECT a.id, a.organization_id, a.first_name, a.last_name, a.middle_name,
                               a.birth_date, a.user_id, a.status, a.enrolled_on, a.note,
                               a.created_at, a.updated_at
                        FROM athlete a
                        WHERE a.organization_id = :organizationId
                          AND (CAST(:q AS text) IS NULL OR position(:q IN lower(concat_ws(' ', a.first_name, a.last_name, a.middle_name))) > 0)
                          AND (CAST(:status AS text) IS NULL OR a.status = :status)
                          AND (CAST(:userId AS uuid) IS NULL OR (:scope IN ('PARENT', 'BOTH') AND EXISTS (
                              SELECT 1 FROM parent_link pl WHERE pl.athlete_id = a.id AND pl.parent_user_id = :userId))
                              OR (:scope IN ('ATHLETE', 'BOTH') AND a.user_id = :userId))
                        ORDER BY a.created_at DESC, a.id DESC LIMIT :limit OFFSET :offset
                        """)
                .param("organizationId", organizationId).param("q", q).param("status", status)
                .param("userId", userId).param("scope", scope).param("limit", limit).param("offset", offset)
                .query((rs, row) -> mapAthlete(rs.getObject("id", UUID.class), rs.getObject("organization_id", UUID.class),
                        rs.getString("first_name"), rs.getString("last_name"), rs.getString("middle_name"),
                        rs.getObject("birth_date", LocalDate.class), rs.getObject("user_id", UUID.class),
                        rs.getString("status"), rs.getObject("enrolled_on", LocalDate.class), rs.getString("note"),
                        rs.getTimestamp("created_at"), rs.getTimestamp("updated_at"), organizationId))
                .list();
    }

    Optional<Athlete> findById(UUID organizationId, UUID athleteId) {
        return jdbcClient.sql("""
                        SELECT id, organization_id, first_name, last_name, middle_name, birth_date, user_id,
                               status, enrolled_on, note, created_at, updated_at
                        FROM athlete WHERE id = :athleteId AND organization_id = :organizationId
                        """)
                .param("athleteId", athleteId).param("organizationId", organizationId)
                .query((rs, row) -> mapAthlete(rs.getObject("id", UUID.class), rs.getObject("organization_id", UUID.class),
                        rs.getString("first_name"), rs.getString("last_name"), rs.getString("middle_name"),
                        rs.getObject("birth_date", LocalDate.class), rs.getObject("user_id", UUID.class),
                        rs.getString("status"), rs.getObject("enrolled_on", LocalDate.class), rs.getString("note"),
                        rs.getTimestamp("created_at"), rs.getTimestamp("updated_at"), organizationId))
                .optional();
    }

    Athlete insert(UUID organizationId, AthleteWrite write, Instant now) {
        UUID id = UUID.randomUUID();
        jdbcClient.sql("""
                        INSERT INTO athlete (id, organization_id, first_name, last_name, middle_name, birth_date,
                            user_id, status, enrolled_on, note, created_at, updated_at)
                        VALUES (:id, :organizationId, :firstName, :lastName, :middleName, :birthDate,
                            :userId, :status, :enrolledOn, :note, :now, :now)
                        """)
                .param("id", id).param("organizationId", organizationId).param("firstName", write.firstName().trim())
                .param("lastName", write.lastName().trim()).param("middleName", write.middleName())
                .param("birthDate", write.birthDate()).param("userId", write.userId()).param("status", write.status())
                .param("enrolledOn", write.enrolledOn()).param("note", write.note()).param("now", JdbcTime.toOffsetDateTime(now)).update();
        return findById(organizationId, id).orElseThrow();
    }

    void patch(UUID organizationId, UUID athleteId, AthletePatch patch, Instant now) {
        jdbcClient.sql("""
                        UPDATE athlete SET first_name = CASE WHEN :firstNameProvided THEN :firstName ELSE first_name END,
                            last_name = CASE WHEN :lastNameProvided THEN :lastName ELSE last_name END,
                            middle_name = CASE WHEN :middleNameProvided THEN :middleName ELSE middle_name END,
                            birth_date = CASE WHEN :birthDateProvided THEN :birthDate ELSE birth_date END,
                            user_id = CASE WHEN :userIdProvided THEN :userId ELSE user_id END,
                            status = CASE WHEN :statusProvided THEN :status ELSE status END,
                            enrolled_on = CASE WHEN :enrolledOnProvided THEN :enrolledOn ELSE enrolled_on END,
                            note = CASE WHEN :noteProvided THEN :note ELSE note END, updated_at = :now
                        WHERE id = :athleteId AND organization_id = :organizationId
                        """)
                .param("firstNameProvided", patch.has("firstName")).param("firstName", patch.firstName())
                .param("lastNameProvided", patch.has("lastName")).param("lastName", patch.lastName())
                .param("middleNameProvided", patch.has("middleName")).param("middleName", patch.middleName())
                .param("birthDateProvided", patch.has("birthDate")).param("birthDate", patch.birthDate())
                .param("userIdProvided", patch.has("userId")).param("userId", patch.userId())
                .param("statusProvided", patch.has("status")).param("status", patch.status())
                .param("enrolledOnProvided", patch.has("enrolledOn")).param("enrolledOn", patch.enrolledOn())
                .param("noteProvided", patch.has("note")).param("note", patch.note())
                .param("now", JdbcTime.toOffsetDateTime(now)).param("athleteId", athleteId).param("organizationId", organizationId).update();
    }

    void replaceParentLinks(UUID organizationId, UUID athleteId, List<ParentLinkWrite> links) {
        jdbcClient.sql("DELETE FROM parent_link WHERE organization_id = :organizationId AND athlete_id = :athleteId")
                .param("organizationId", organizationId).param("athleteId", athleteId).update();
        for (ParentLinkWrite link : links) {
            jdbcClient.sql("""
                    INSERT INTO parent_link (organization_id, athlete_id, parent_user_id, relationship)
                    VALUES (:organizationId, :athleteId, :parentUserId, :relationship)
                    """).param("organizationId", organizationId).param("athleteId", athleteId)
                    .param("parentUserId", link.parentUserId()).param("relationship", link.relationship()).update();
        }
    }

    boolean hasActiveRole(UUID userId, UUID organizationId, String role) {
        return jdbcClient.sql("""
                SELECT count(*) FROM membership
                WHERE user_id = :userId AND organization_id = :organizationId AND status = 'ACTIVE'
                  AND roles @> jsonb_build_array(CAST(:role AS text))
                """).param("userId", userId).param("organizationId", organizationId).param("role", role)
                .query(Long.class).single() > 0;
    }

    private Athlete mapAthlete(UUID id, UUID organizationId, String firstName, String lastName, String middleName,
                               LocalDate birthDate, UUID userId, String status, LocalDate enrolledOn, String note,
                               Timestamp createdAt, Timestamp updatedAt, UUID ignoredOrganizationId) {
        List<ParentLink> links = jdbcClient.sql("""
                        SELECT pl.parent_user_id, u.full_name, u.email, pl.relationship
                        FROM parent_link pl JOIN app_user u ON u.id = pl.parent_user_id
                        WHERE pl.organization_id = :organizationId AND pl.athlete_id = :athleteId
                        ORDER BY u.full_name
                        """).param("organizationId", organizationId).param("athleteId", id)
                .query((rs, row) -> new ParentLink(rs.getObject("parent_user_id", UUID.class), rs.getString("full_name"),
                        rs.getString("email"), rs.getString("relationship"))).list();
        return new Athlete(id, organizationId, firstName, lastName, middleName, birthDate, userId, status,
                enrolledOn, note, links, createdAt.toInstant(), updatedAt.toInstant());
    }

    private List<String> readList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() { });
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Stored membership data is invalid", exception);
        }
    }

    record MembershipAccess(List<String> roles, List<String> permissions) {
        boolean hasPermission(String permission) { return permissions.contains(permission); }
        boolean hasRole(String role) { return roles.contains(role); }
    }
}