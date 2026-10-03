package ru.sportorg.dictionaries;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ru.sportorg.jdbc.JdbcTime;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class DictionaryRepository {
    private final JdbcClient jdbc;
    DictionaryRepository(JdbcClient jdbc) { this.jdbc = jdbc; }
    boolean organizationExists(UUID org) { return jdbc.sql("SELECT count(*) FROM organization WHERE id = :org").param("org", org).query(Long.class).single() > 0; }
    Membership membership(UUID user, UUID org) { return jdbc.sql("SELECT roles::text roles, permissions::text permissions FROM membership WHERE user_id = :user AND organization_id = :org AND status = 'ACTIVE'").param("user", user).param("org", org).query((rs, row) -> new Membership(rs.getString("roles"), rs.getString("permissions"))).optional().orElse(null); }
    long count(UUID org, String type, String q, String status) { return jdbc.sql("SELECT count(*) FROM dictionary_item WHERE organization_id = :org AND dictionary_type = :type AND (CAST(:q AS text) IS NULL OR position(:q IN lower(name)) > 0) AND (CAST(:status AS text) IS NULL OR status = :status)").param("org", org).param("type", type).param("q", q).param("status", status).query(Long.class).single(); }
    List<DictionaryItem> find(UUID org, String type, String q, String status, int limit, int offset) { return jdbc.sql("SELECT id, organization_id, dictionary_type, name, description, address, sort_order, status, created_at, updated_at FROM dictionary_item WHERE organization_id = :org AND dictionary_type = :type AND (CAST(:q AS text) IS NULL OR position(:q IN lower(name)) > 0) AND (CAST(:status AS text) IS NULL OR status = :status) ORDER BY sort_order, name, id LIMIT :limit OFFSET :offset").param("org", org).param("type", type).param("q", q).param("status", status).param("limit", limit).param("offset", offset).query((rs, row) -> item(rs)).list(); }
    DictionaryItem insert(UUID org, String type, DictionaryWrite w) { UUID id = UUID.randomUUID(); jdbc.sql("INSERT INTO dictionary_item (id, organization_id, dictionary_type, name, description, address, sort_order, status) VALUES (:id, :org, :type, :name, :description, :address, :sortOrder, :status)").param("id", id).param("org", org).param("type", type).param("name", w.name().trim()).param("description", w.description()).param("address", w.address()).param("sortOrder", w.sortOrder()).param("status", w.status()).update(); return findById(org, type, id).orElseThrow(); }
    Optional<DictionaryItem> findById(UUID org, String type, UUID id) { return jdbc.sql("SELECT id, organization_id, dictionary_type, name, description, address, sort_order, status, created_at, updated_at FROM dictionary_item WHERE organization_id = :org AND dictionary_type = :type AND id = :id").param("org", org).param("type", type).param("id", id).query((rs, row) -> item(rs)).optional(); }
    void patch(UUID org, String type, UUID id, DictionaryWrite w, Instant now) { jdbc.sql("UPDATE dictionary_item SET name = :name, description = :description, address = :address, sort_order = :sortOrder, status = :status, updated_at = :now WHERE organization_id = :org AND dictionary_type = :type AND id = :id").param("name", w.name()).param("description", w.description()).param("address", w.address()).param("sortOrder", w.sortOrder()).param("status", w.status()).param("now", JdbcTime.toOffsetDateTime(now)).param("org", org).param("type", type).param("id", id).update(); }
    private DictionaryItem item(java.sql.ResultSet rs) throws java.sql.SQLException { return new DictionaryItem(rs.getObject("id", UUID.class), rs.getObject("organization_id", UUID.class), rs.getString("dictionary_type"), rs.getString("name"), rs.getString("description"), rs.getString("address"), rs.getInt("sort_order"), rs.getString("status"), rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant()); }
    record Membership(String roles, String permissions) { boolean permission(String value) { return permissions.contains(value); } boolean role(String value) { return roles.contains(value); } }
}