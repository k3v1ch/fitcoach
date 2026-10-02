package ru.sportorg.reports;

import java.sql.ResultSetMetaData;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class ReportRepository {
    private final JdbcClient jdbc;
    ReportRepository(JdbcClient jdbc) { this.jdbc = jdbc; }
    boolean organizationExists(UUID org) { return jdbc.sql("SELECT count(*) FROM organization WHERE id = :org").param("org", org).query(Long.class).single() > 0; }
    Membership membership(UUID user, UUID org) { return jdbc.sql("SELECT roles::text roles, permissions::text permissions FROM membership WHERE user_id = :user AND organization_id = :org AND status = 'ACTIVE'").param("user", user).param("org", org).query((rs, row) -> new Membership(rs.getString("roles"), rs.getString("permissions"))).optional().orElse(null); }
    List<Map<String, Object>> rows(ReportType type, UUID org, LocalDate from, LocalDate to, int limit) {
        String sql = switch (type) {
            case ATTENDANCE -> "SELECT a.training_id, t.starts_at, a.athlete_id, a.status attendance_status, a.reason FROM attendance a JOIN training t ON t.id = a.training_id WHERE t.organization_id = :org AND t.starts_at::date BETWEEN :fromDate AND :toDate ORDER BY t.starts_at, a.athlete_id LIMIT :limit";
            case TRAININGS -> "SELECT t.id training_id, t.starts_at, t.ends_at, t.title, t.status, r.status report_status FROM training t LEFT JOIN training_report r ON r.training_id = t.id WHERE t.organization_id = :org AND t.starts_at::date BETWEEN :fromDate AND :toDate ORDER BY t.starts_at, t.id LIMIT :limit";
            case PROGRESS -> "SELECT id, athlete_id, measured_on, metric_name, value, unit, is_personal_best, comment FROM athlete_result WHERE organization_id = :org AND measured_on BETWEEN :fromDate AND :toDate ORDER BY measured_on, id LIMIT :limit";
            case CHARGES -> "SELECT c.id charge_id, c.athlete_id, c.title, c.due_on, c.amount, c.status, COALESCE((SELECT SUM(p.amount) FROM payment p WHERE p.charge_id = c.id AND p.status = 'ACTIVE'), 0) paid_amount FROM charge c WHERE c.organization_id = :org AND c.due_on BETWEEN :fromDate AND :toDate ORDER BY c.due_on, c.id LIMIT :limit";
        };
        return jdbc.sql(sql).param("org", org).param("fromDate", from).param("toDate", to).param("limit", limit).query((rs, row) -> map(rs)).list();
    }
    private Map<String, Object> map(java.sql.ResultSet rs) throws java.sql.SQLException { ResultSetMetaData metadata = rs.getMetaData(); Map<String, Object> result = new LinkedHashMap<>(); for (int i = 1; i <= metadata.getColumnCount(); i++) result.put(metadata.getColumnLabel(i), rs.getObject(i)); return result; }
    record Membership(String roles, String permissions) { boolean permission(String value) { return permissions.contains(value); } }
}