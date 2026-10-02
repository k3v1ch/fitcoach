package ru.sportorg.groups;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class GroupRepository {
    private final JdbcClient jdbc;
    private final ObjectMapper mapper;

    GroupRepository(JdbcClient jdbc, ObjectMapper mapper) { this.jdbc = jdbc; this.mapper = mapper; }

    boolean organizationExists(UUID id) { return jdbc.sql("SELECT count(*) FROM organization WHERE id = :id")
            .param("id", id).query(Long.class).single() > 0; }

    Optional<MembershipAccess> membership(UUID userId, UUID organizationId) {
        return jdbc.sql("SELECT roles::text roles, permissions::text permissions FROM membership WHERE user_id = :userId AND organization_id = :organizationId AND status = 'ACTIVE'")
                .param("userId", userId).param("organizationId", organizationId)
                .query((rs, row) -> new MembershipAccess(read(rs.getString("roles")), read(rs.getString("permissions")))).optional();
    }

    long countSections(UUID org, String q, UUID sportTypeId, String status) {
        return jdbc.sql("SELECT count(*) FROM section WHERE organization_id = :org AND (:q IS NULL OR position(:q IN lower(name)) > 0) AND (:sportTypeId IS NULL OR sport_type_id = :sportTypeId) AND (:status IS NULL OR status = :status)")
                .param("org", org).param("q", q).param("sportTypeId", sportTypeId).param("status", status).query(Long.class).single();
    }

    List<Section> sections(UUID org, String q, UUID sportTypeId, String status, int limit, int offset) {
        return jdbc.sql("SELECT id, organization_id, name, sport_type_id, description, status, created_at, updated_at FROM section WHERE organization_id = :org AND (:q IS NULL OR position(:q IN lower(name)) > 0) AND (:sportTypeId IS NULL OR sport_type_id = :sportTypeId) AND (:status IS NULL OR status = :status) ORDER BY created_at DESC, id DESC LIMIT :limit OFFSET :offset")
                .param("org", org).param("q", q).param("sportTypeId", sportTypeId).param("status", status).param("limit", limit).param("offset", offset)
                .query((rs, row) -> section(rs.getObject("id", UUID.class), rs.getObject("organization_id", UUID.class), rs.getString("name"), rs.getObject("sport_type_id", UUID.class), rs.getString("description"), rs.getString("status"), rs.getTimestamp("created_at"), rs.getTimestamp("updated_at"))).list();
    }

    Optional<Section> section(UUID org, UUID id) {
        return jdbc.sql("SELECT id, organization_id, name, sport_type_id, description, status, created_at, updated_at FROM section WHERE organization_id = :org AND id = :id")
                .param("org", org).param("id", id).query((rs, row) -> section(rs.getObject("id", UUID.class), rs.getObject("organization_id", UUID.class), rs.getString("name"), rs.getObject("sport_type_id", UUID.class), rs.getString("description"), rs.getString("status"), rs.getTimestamp("created_at"), rs.getTimestamp("updated_at"))).optional();
    }

    Section insertSection(UUID org, SectionWrite write, Instant now) {
        UUID id = UUID.randomUUID();
        jdbc.sql("INSERT INTO section (id, organization_id, name, sport_type_id, description, status, created_at, updated_at) VALUES (:id, :org, :name, :sportTypeId, :description, :status, :now, :now)")
                .param("id", id).param("org", org).param("name", write.name().trim()).param("sportTypeId", write.sportTypeId()).param("description", write.description()).param("status", write.status()).param("now", now).update();
        return section(org, id).orElseThrow();
    }

    void patchSection(UUID org, UUID id, SectionPatch patch, Instant now) {
        jdbc.sql("UPDATE section SET name = CASE WHEN :nameProvided THEN :name ELSE name END, sport_type_id = CASE WHEN :sportTypeProvided THEN :sportTypeId ELSE sport_type_id END, description = CASE WHEN :descriptionProvided THEN :description ELSE description END, status = CASE WHEN :statusProvided THEN :status ELSE status END, updated_at = :now WHERE organization_id = :org AND id = :id")
                .param("nameProvided", patch.has("name")).param("name", patch.name()).param("sportTypeProvided", patch.has("sportTypeId")).param("sportTypeId", patch.sportTypeId()).param("descriptionProvided", patch.has("description")).param("description", patch.description()).param("statusProvided", patch.has("status")).param("status", patch.status()).param("now", now).param("org", org).param("id", id).update();
    }

    long countGroups(UUID org, String q, UUID sectionId, UUID coachId, UUID athleteId, String status, UUID scopeUser, String scope) {
        return jdbc.sql(groupWhere("SELECT count(*) FROM sport_group g", false))
                .param("org", org).param("q", q).param("sectionId", sectionId).param("coachId", coachId).param("athleteId", athleteId).param("status", status).param("scopeUser", scopeUser).param("scope", scope).query(Long.class).single();
    }

    List<Group> groups(UUID org, String q, UUID sectionId, UUID coachId, UUID athleteId, String status, UUID scopeUser, String scope, int limit, int offset) {
        return jdbc.sql(groupWhere("SELECT g.id, g.organization_id, g.section_id, g.name, g.description, g.status, g.created_at, g.updated_at FROM sport_group g", false) + " ORDER BY g.created_at DESC, g.id DESC LIMIT :limit OFFSET :offset")
                .param("org", org).param("q", q).param("sectionId", sectionId).param("coachId", coachId).param("athleteId", athleteId).param("status", status).param("scopeUser", scopeUser).param("scope", scope).param("limit", limit).param("offset", offset)
                .query((rs, row) -> mapGroup(rs.getObject("id", UUID.class), rs.getObject("organization_id", UUID.class), rs.getObject("section_id", UUID.class), rs.getString("name"), rs.getString("description"), rs.getString("status"), rs.getTimestamp("created_at"), rs.getTimestamp("updated_at"))).list();
    }

    Optional<Group> group(UUID org, UUID id) {
        return jdbc.sql("SELECT g.id, g.organization_id, g.section_id, g.name, g.description, g.status, g.created_at, g.updated_at FROM sport_group g WHERE g.organization_id = :org AND g.id = :id")
                .param("org", org).param("id", id).query((rs, row) -> mapGroup(rs.getObject("id", UUID.class), rs.getObject("organization_id", UUID.class), rs.getObject("section_id", UUID.class), rs.getString("name"), rs.getString("description"), rs.getString("status"), rs.getTimestamp("created_at"), rs.getTimestamp("updated_at"))).optional();
    }

    Group insertGroup(UUID org, GroupWrite write, Instant now) {
        UUID id = UUID.randomUUID();
        jdbc.sql("INSERT INTO sport_group (id, organization_id, section_id, name, description, status, created_at, updated_at) VALUES (:id, :org, :sectionId, :name, :description, :status, :now, :now)")
                .param("id", id).param("org", org).param("sectionId", write.sectionId()).param("name", write.name().trim()).param("description", write.description()).param("status", write.status()).param("now", now).update();
        replaceCoaches(id, write.coachIds());
        return group(org, id).orElseThrow();
    }

    void patchGroup(UUID org, UUID id, GroupPatch patch, Instant now) {
        jdbc.sql("UPDATE sport_group SET name = CASE WHEN :nameProvided THEN :name ELSE name END, description = CASE WHEN :descriptionProvided THEN :description ELSE description END, status = CASE WHEN :statusProvided THEN :status ELSE status END, updated_at = :now WHERE organization_id = :org AND id = :id")
                .param("nameProvided", patch.has("name")).param("name", patch.name()).param("descriptionProvided", patch.has("description")).param("description", patch.description()).param("statusProvided", patch.has("status")).param("status", patch.status()).param("now", now).param("org", org).param("id", id).update();
        if (patch.has("coachIds")) replaceCoaches(id, patch.coachIds());
    }

    void replaceCoaches(UUID groupId, List<UUID> coachIds) {
        jdbc.sql("DELETE FROM group_coach WHERE group_id = :groupId").param("groupId", groupId).update();
        for (UUID coachId : coachIds) jdbc.sql("INSERT INTO group_coach (group_id, coach_id) VALUES (:groupId, :coachId)").param("groupId", groupId).param("coachId", coachId).update();
    }

    boolean activeRole(UUID userId, UUID org, String role) { return jdbc.sql("SELECT count(*) FROM membership WHERE user_id = :userId AND organization_id = :org AND status = 'ACTIVE' AND roles @> jsonb_build_array(:role)").param("userId", userId).param("org", org).param("role", role).query(Long.class).single() > 0; }
    boolean athleteInOrg(UUID athleteId, UUID org) { return jdbc.sql("SELECT count(*) FROM athlete WHERE id = :athleteId AND organization_id = :org").param("athleteId", athleteId).param("org", org).query(Long.class).single() > 0; }
    boolean sectionActive(UUID sectionId, UUID org) { return jdbc.sql("SELECT count(*) FROM section WHERE id = :id AND organization_id = :org AND status = 'ACTIVE'").param("id", sectionId).param("org", org).query(Long.class).single() > 0; }
    boolean groupOpen(UUID groupId, UUID athleteId) { return jdbc.sql("SELECT count(*) FROM group_athlete WHERE group_id = :groupId AND athlete_id = :athleteId AND left_on IS NULL").param("groupId", groupId).param("athleteId", athleteId).query(Long.class).single() > 0; }
    boolean groupHasAthlete(UUID groupId, UUID athleteId) { return jdbc.sql("SELECT count(*) FROM group_athlete WHERE group_id = :groupId AND athlete_id = :athleteId").param("groupId", groupId).param("athleteId", athleteId).query(Long.class).single() > 0; }
    void addAthlete(UUID org, UUID groupId, UUID athleteId, LocalDate joinedOn) { jdbc.sql("INSERT INTO group_athlete (organization_id, group_id, athlete_id, joined_on) VALUES (:org, :groupId, :athleteId, :joinedOn)").param("org", org).param("groupId", groupId).param("athleteId", athleteId).param("joinedOn", joinedOn).update(); }
    void leaveAthlete(UUID groupId, UUID athleteId, LocalDate leftOn) { jdbc.sql("UPDATE group_athlete SET left_on = :leftOn WHERE group_id = :groupId AND athlete_id = :athleteId AND left_on IS NULL").param("leftOn", leftOn).param("groupId", groupId).param("athleteId", athleteId).update(); }

    List<GroupAthlete> athletes(UUID groupId, boolean includeFormer, UUID scopeUser, String scope) {
        String scopeSql = "";
        if (scopeUser != null) scopeSql = " AND ((:scope IN ('PARENT','BOTH') AND EXISTS (SELECT 1 FROM parent_link pl WHERE pl.athlete_id = a.id AND pl.parent_user_id = :scopeUser)) OR (:scope IN ('ATHLETE','BOTH') AND a.user_id = :scopeUser))";
        String formerSql = includeFormer ? "" : " AND ga.left_on IS NULL";
        return jdbc.sql("SELECT a.id, concat_ws(' ', a.first_name, a.last_name, a.middle_name) full_name, ga.joined_on, ga.left_on FROM group_athlete ga JOIN athlete a ON a.id = ga.athlete_id WHERE ga.group_id = :groupId" + formerSql + scopeSql + " ORDER BY ga.joined_on, a.id")
                .param("groupId", groupId).param("scopeUser", scopeUser).param("scope", scope).query((rs, row) -> new GroupAthlete(rs.getObject("id", UUID.class), rs.getString("full_name"), rs.getObject("joined_on", LocalDate.class), rs.getObject("left_on", LocalDate.class))).list();
    }

    private String groupWhere(String select, boolean unused) { return select + " WHERE g.organization_id = :org AND (:q IS NULL OR position(:q IN lower(g.name)) > 0) AND (:sectionId IS NULL OR g.section_id = :sectionId) AND (:coachId IS NULL OR EXISTS (SELECT 1 FROM group_coach gc WHERE gc.group_id = g.id AND gc.coach_id = :coachId)) AND (:athleteId IS NULL OR EXISTS (SELECT 1 FROM group_athlete ga WHERE ga.group_id = g.id AND ga.athlete_id = :athleteId AND ga.left_on IS NULL)) AND (:status IS NULL OR g.status = :status) AND (:scopeUser IS NULL OR (:scope IN ('PARENT','BOTH') AND EXISTS (SELECT 1 FROM group_athlete ga JOIN parent_link pl ON pl.athlete_id = ga.athlete_id WHERE ga.group_id = g.id AND ga.left_on IS NULL AND pl.parent_user_id = :scopeUser)) OR (:scope IN ('ATHLETE','BOTH') AND EXISTS (SELECT 1 FROM group_athlete ga JOIN athlete a ON a.id = ga.athlete_id WHERE ga.group_id = g.id AND ga.left_on IS NULL AND a.user_id = :scopeUser)))"; }
    private Group mapGroup(UUID id, UUID org, UUID sectionId, String name, String description, String status, Timestamp created, Timestamp updated) {
        List<UUID> coaches = jdbc.sql("SELECT coach_id FROM group_coach WHERE group_id = :id ORDER BY coach_id").param("id", id).query(UUID.class).list();
        int count = jdbc.sql("SELECT count(*) FROM group_athlete WHERE group_id = :id AND left_on IS NULL").param("id", id).query(Integer.class).single();
        return new Group(id, org, sectionId, name, coaches, description, status, count, created.toInstant(), updated.toInstant());
    }
    private Section section(UUID id, UUID org, String name, UUID sportTypeId, String description, String status, Timestamp created, Timestamp updated) { return new Section(id, org, name, sportTypeId, description, status, created.toInstant(), updated.toInstant()); }
    private List<String> read(String json) { try { return mapper.readValue(json, new TypeReference<>() { }); } catch (java.io.IOException e) { throw new IllegalStateException("Stored membership data is invalid", e); } }
    record MembershipAccess(List<String> roles, List<String> permissions) { boolean role(String role) { return roles.contains(role); } boolean permission(String permission) { return permissions.contains(permission); } }
}