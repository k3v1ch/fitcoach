package ru.sportorg.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class FinanceRepository {
    private final JdbcClient jdbc;
    FinanceRepository(JdbcClient jdbc) { this.jdbc = jdbc; }
    boolean organizationExists(UUID org) { return jdbc.sql("SELECT count(*) FROM organization WHERE id = :org").param("org", org).query(Long.class).single() > 0; }
    Membership membership(UUID user, UUID org) { return jdbc.sql("SELECT roles::text roles, permissions::text permissions FROM membership WHERE user_id = :user AND organization_id = :org AND status = 'ACTIVE'").param("user", user).param("org", org).query((rs, row) -> new Membership(rs.getString("roles"), rs.getString("permissions"))).optional().orElse(null); }
    boolean athleteExists(UUID org, UUID athlete) { return jdbc.sql("SELECT count(*) FROM athlete WHERE organization_id = :org AND id = :athlete").param("org", org).param("athlete", athlete).query(Long.class).single() > 0; }
    boolean athleteVisible(UUID org, UUID athlete, UUID user) { return jdbc.sql("SELECT count(*) FROM athlete a WHERE a.organization_id = :org AND a.id = :athlete AND (a.user_id = :user OR EXISTS (SELECT 1 FROM parent_link pl WHERE pl.athlete_id = a.id AND pl.parent_user_id = :user))").param("org", org).param("athlete", athlete).param("user", user).query(Long.class).single() > 0; }
    Charge insertCharge(UUID org, ChargeWrite w, UUID user) { UUID id = UUID.randomUUID(); jdbc.sql("INSERT INTO charge (id, organization_id, athlete_id, section_id, type, title, amount, due_on, period_from, period_to, training_id, event_id, comment, status, created_by) VALUES (:id, :org, :athlete, :section, :type, :title, :amount, :due, :fromDate, :toDate, :training, :event, :comment, 'ACTIVE', :user)").param("id", id).param("org", org).param("athlete", w.athleteId()).param("section", w.sectionId()).param("type", w.type()).param("title", w.title()).param("amount", w.amount()).param("due", w.dueOn()).param("fromDate", w.periodFrom()).param("toDate", w.periodTo()).param("training", w.trainingId()).param("event", w.eventId()).param("comment", w.comment()).param("user", user).update(); return charge(org, id).orElseThrow(); }
    Optional<Charge> charge(UUID org, UUID id) { return jdbc.sql("SELECT c.*, COALESCE((SELECT SUM(p.amount) FROM payment p WHERE p.charge_id = c.id AND p.status = 'ACTIVE'), 0) paid FROM charge c WHERE c.organization_id = :org AND c.id = :id").param("org", org).param("id", id).query((rs, row) -> mapCharge(rs)).optional(); }
    Optional<Charge> lockCharge(UUID org, UUID id) { return jdbc.sql("SELECT c.*, COALESCE((SELECT SUM(p.amount) FROM payment p WHERE p.charge_id = c.id AND p.status = 'ACTIVE'), 0) paid FROM charge c WHERE c.organization_id = :org AND c.id = :id FOR UPDATE").param("org", org).param("id", id).query((rs, row) -> mapCharge(rs)).optional(); }
    Optional<Payment> byKey(UUID org, String key) { return jdbc.sql("SELECT id, organization_id, charge_id, athlete_id, section_id, amount, paid_on, method, comment, status, created_by, created_at, voided_by, voided_at, void_reason FROM payment WHERE organization_id = :org AND idempotency_key = :key").param("org", org).param("key", key).query((rs, row) -> payment(rs)).optional(); }
    long countPayments(UUID org, UUID paymentId, UUID athleteId, UUID sectionId, UUID chargeId, LocalDate from, LocalDate to, String status, UUID scopeUserId) {
        return jdbc.sql("SELECT count(*) FROM payment p WHERE p.organization_id = :org AND (CAST(:paymentId AS uuid) IS NULL OR p.id = :paymentId) AND (CAST(:athleteId AS uuid) IS NULL OR p.athlete_id = :athleteId) AND (CAST(:sectionId AS uuid) IS NULL OR p.section_id = :sectionId) AND (CAST(:chargeId AS uuid) IS NULL OR p.charge_id = :chargeId) AND (CAST(:fromDate AS date) IS NULL OR p.paid_on >= :fromDate) AND (CAST(:toDate AS date) IS NULL OR p.paid_on <= :toDate) AND (CAST(:status AS text) IS NULL OR p.status = :status) AND (CAST(:scopeUserId AS uuid) IS NULL OR EXISTS (SELECT 1 FROM athlete a WHERE a.organization_id = p.organization_id AND a.id = p.athlete_id AND (a.user_id = :scopeUserId OR EXISTS (SELECT 1 FROM parent_link pl WHERE pl.athlete_id = a.id AND pl.parent_user_id = :scopeUserId))))")
                .param("org", org).param("paymentId", paymentId).param("athleteId", athleteId).param("sectionId", sectionId)
                .param("chargeId", chargeId).param("fromDate", from).param("toDate", to).param("status", status)
                .param("scopeUserId", scopeUserId).query(Long.class).single();
    }
    List<Payment> payments(UUID org, UUID paymentId, UUID athleteId, UUID sectionId, UUID chargeId, LocalDate from, LocalDate to, String status, UUID scopeUserId, int limit, int offset) {
        return jdbc.sql("SELECT p.id, p.organization_id, p.charge_id, p.athlete_id, p.section_id, p.amount, p.paid_on, p.method, p.comment, p.status, p.created_by, p.created_at, p.voided_by, p.voided_at, p.void_reason FROM payment p WHERE p.organization_id = :org AND (CAST(:paymentId AS uuid) IS NULL OR p.id = :paymentId) AND (CAST(:athleteId AS uuid) IS NULL OR p.athlete_id = :athleteId) AND (CAST(:sectionId AS uuid) IS NULL OR p.section_id = :sectionId) AND (CAST(:chargeId AS uuid) IS NULL OR p.charge_id = :chargeId) AND (CAST(:fromDate AS date) IS NULL OR p.paid_on >= :fromDate) AND (CAST(:toDate AS date) IS NULL OR p.paid_on <= :toDate) AND (CAST(:status AS text) IS NULL OR p.status = :status) AND (CAST(:scopeUserId AS uuid) IS NULL OR EXISTS (SELECT 1 FROM athlete a WHERE a.organization_id = p.organization_id AND a.id = p.athlete_id AND (a.user_id = :scopeUserId OR EXISTS (SELECT 1 FROM parent_link pl WHERE pl.athlete_id = a.id AND pl.parent_user_id = :scopeUserId)))) ORDER BY p.created_at DESC, p.id DESC LIMIT :limit OFFSET :offset")
                .param("org", org).param("paymentId", paymentId).param("athleteId", athleteId).param("sectionId", sectionId)
                .param("chargeId", chargeId).param("fromDate", from).param("toDate", to).param("status", status)
                .param("scopeUserId", scopeUserId).param("limit", limit).param("offset", offset)
                .query((rs, row) -> payment(rs)).list();
    }
    FinanceSummary summary(UUID org, LocalDate from, LocalDate to) { return jdbc.sql("SELECT COALESCE((SELECT SUM(p.amount) FROM payment p WHERE p.organization_id = :org AND p.status = 'ACTIVE' AND p.paid_on BETWEEN :fromDate AND :toDate), 0) received, COALESCE((SELECT SUM(c.amount - COALESCE((SELECT SUM(p2.amount) FROM payment p2 WHERE p2.charge_id = c.id AND p2.status = 'ACTIVE'), 0)) FROM charge c WHERE c.organization_id = :org AND c.status = 'ACTIVE'), 0) outstanding, COALESCE((SELECT SUM(c.amount - COALESCE((SELECT SUM(p3.amount) FROM payment p3 WHERE p3.charge_id = c.id AND p3.status = 'ACTIVE'), 0)) FROM charge c WHERE c.organization_id = :org AND c.status = 'ACTIVE' AND c.due_on < CURRENT_DATE), 0) overdue").param("org", org).param("fromDate", from).param("toDate", to).query((rs, row) -> new FinanceSummary(from, to, "RUB", rs.getBigDecimal("received"), rs.getBigDecimal("outstanding"), rs.getBigDecimal("overdue"))).single(); }
    Payment insertPayment(UUID org, Charge charge, PaymentWrite w, UUID user, String key) { UUID id = UUID.randomUUID(); jdbc.sql("INSERT INTO payment (id, organization_id, charge_id, athlete_id, section_id, amount, paid_on, method, comment, status, idempotency_key, created_by) VALUES (:id, :org, :charge, :athlete, :section, :amount, :paidOn, :method, :comment, 'ACTIVE', :key, :user)").param("id", id).param("org", org).param("charge", w.chargeId()).param("athlete", charge.athleteId()).param("section", charge.sectionId()).param("amount", w.amount()).param("paidOn", w.paidOn()).param("method", w.method()).param("comment", w.comment()).param("key", key).param("user", user).update(); return byKey(org, key).orElseThrow(); }
    private Charge mapCharge(java.sql.ResultSet rs) throws java.sql.SQLException { BigDecimal amount = rs.getBigDecimal("amount"); BigDecimal paid = rs.getBigDecimal("paid"); BigDecimal remaining = amount.subtract(paid); String paymentStatus = paid.signum() == 0 ? "UNPAID" : remaining.signum() == 0 ? "PAID" : "PARTIALLY_PAID"; boolean overdue = "ACTIVE".equals(rs.getString("status")) && remaining.signum() > 0 && rs.getObject("due_on", LocalDate.class).isBefore(LocalDate.now()); return new Charge(rs.getObject("id", UUID.class), rs.getObject("organization_id", UUID.class), rs.getObject("athlete_id", UUID.class), rs.getObject("section_id", UUID.class), rs.getString("type"), rs.getString("title"), amount, rs.getObject("due_on", LocalDate.class), paid, remaining, paymentStatus, overdue, rs.getString("status"), rs.getObject("period_from", LocalDate.class), rs.getObject("period_to", LocalDate.class), rs.getObject("training_id", UUID.class), rs.getObject("event_id", UUID.class), rs.getString("comment"), rs.getString("cancel_reason"), rs.getObject("cancelled_by", UUID.class), instant(rs, "cancelled_at"), rs.getObject("created_by", UUID.class), instant(rs, "created_at"), instant(rs, "updated_at")); }
private static java.time.Instant instant(java.sql.ResultSet rs, String column) throws java.sql.SQLException { java.sql.Timestamp value = rs.getTimestamp(column); return value == null ? null : value.toInstant(); }
    private Payment payment(java.sql.ResultSet rs) throws java.sql.SQLException { return new Payment(rs.getObject("id", UUID.class), rs.getObject("organization_id", UUID.class), rs.getObject("charge_id", UUID.class), rs.getObject("athlete_id", UUID.class), rs.getObject("section_id", UUID.class), rs.getBigDecimal("amount"), rs.getObject("paid_on", LocalDate.class), rs.getString("method"), rs.getString("comment"), rs.getString("status"), rs.getObject("created_by", UUID.class), rs.getTimestamp("created_at").toInstant(), rs.getObject("voided_by", UUID.class), rs.getTimestamp("voided_at") == null ? null : rs.getTimestamp("voided_at").toInstant(), rs.getString("void_reason")); }
        // Список начислений: статус оплаты и просрочка вычисляются по действующим платежам
    private static final String CHARGES_FROM = "WITH c AS (SELECT c.*, COALESCE((SELECT SUM(p.amount) FROM payment p WHERE p.charge_id = c.id AND p.status = 'ACTIVE'), 0) paid FROM charge c WHERE c.organization_id = :org) ";
    private static final String CHARGES_WHERE = " WHERE (CAST(:q AS text) IS NULL OR position(lower(:q) IN lower(c.title)) > 0)"
            + " AND (CAST(:athleteId AS uuid) IS NULL OR c.athlete_id = :athleteId)"
            + " AND (CAST(:sectionId AS uuid) IS NULL OR c.section_id = :sectionId)"
            + " AND (CAST(:type AS text) IS NULL OR c.type = :type)"
            + " AND (CAST(:eventId AS uuid) IS NULL OR c.event_id = :eventId)"
            + " AND (CAST(:paymentStatus AS text) IS NULL OR (CASE WHEN c.paid = 0 THEN 'UNPAID' WHEN c.paid >= c.amount THEN 'PAID' ELSE 'PARTIALLY_PAID' END) = :paymentStatus)"
            + " AND (CAST(:overdue AS boolean) IS NULL OR (c.status = 'ACTIVE' AND c.amount - c.paid > 0 AND c.due_on < CURRENT_DATE) = CAST(:overdue AS boolean))"
            + " AND (CAST(:status AS text) IS NULL OR c.status = :status)"
            + " AND (CAST(:dueFrom AS date) IS NULL OR c.due_on >= :dueFrom)"
            + " AND (CAST(:dueTo AS date) IS NULL OR c.due_on <= :dueTo)"
            + " AND (CAST(:scopeUserId AS uuid) IS NULL OR EXISTS (SELECT 1 FROM athlete a WHERE a.organization_id = c.organization_id AND a.id = c.athlete_id AND (a.user_id = :scopeUserId OR EXISTS (SELECT 1 FROM parent_link pl WHERE pl.athlete_id = a.id AND pl.parent_user_id = :scopeUserId))))";

    long countCharges(UUID org, String q, UUID athleteId, UUID sectionId, String type, UUID eventId, String paymentStatus, Boolean overdue, String status, LocalDate dueFrom, LocalDate dueTo, UUID scopeUserId) {
        return chargeFilters(jdbc.sql(CHARGES_FROM + "SELECT count(*) FROM c" + CHARGES_WHERE), org, q, athleteId, sectionId, type, eventId, paymentStatus, overdue, status, dueFrom, dueTo, scopeUserId)
                .query(Long.class).single();
    }
    List<Charge> charges(UUID org, String q, UUID athleteId, UUID sectionId, String type, UUID eventId, String paymentStatus, Boolean overdue, String status, LocalDate dueFrom, LocalDate dueTo, UUID scopeUserId, int limit, int offset) {
        return chargeFilters(jdbc.sql(CHARGES_FROM + "SELECT c.* FROM c" + CHARGES_WHERE + " ORDER BY c.created_at DESC, c.id DESC LIMIT :limit OFFSET :offset"), org, q, athleteId, sectionId, type, eventId, paymentStatus, overdue, status, dueFrom, dueTo, scopeUserId)
                .param("limit", limit).param("offset", offset)
                .query((rs, row) -> mapCharge(rs)).list();
    }
    private JdbcClient.StatementSpec chargeFilters(JdbcClient.StatementSpec spec, UUID org, String q, UUID athleteId, UUID sectionId, String type, UUID eventId, String paymentStatus, Boolean overdue, String status, LocalDate dueFrom, LocalDate dueTo, UUID scopeUserId) {
        return spec.param("org", org).param("q", q).param("athleteId", athleteId).param("sectionId", sectionId).param("type", type)
                .param("eventId", eventId).param("paymentStatus", paymentStatus).param("overdue", overdue).param("status", status)
                .param("dueFrom", dueFrom).param("dueTo", dueTo).param("scopeUserId", scopeUserId);
    }
    void cancelCharge(UUID org, UUID id, String reason, UUID user) {
        jdbc.sql("UPDATE charge SET status = 'CANCELLED', cancel_reason = :reason, cancelled_by = :user, cancelled_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE organization_id = :org AND id = :id")
                .param("reason", reason).param("user", user).param("org", org).param("id", id).update();
    }
    void updateCharge(UUID org, UUID id, ChargePatch patch, UUID user) {
        List<String> sets = new java.util.ArrayList<>();
        if (patch.has("title")) sets.add("title = :title");
        if (patch.has("amount")) sets.add("amount = :amount");
        if (patch.has("dueOn")) sets.add("due_on = :dueOn");
        if (patch.has("comment")) sets.add("comment = :comment");
        sets.add("updated_at = CURRENT_TIMESTAMP");
        JdbcClient.StatementSpec spec = jdbc.sql("UPDATE charge SET " + String.join(", ", sets) + " WHERE organization_id = :org AND id = :id")
                .param("org", org).param("id", id);
        if (patch.has("title")) spec = spec.param("title", patch.title().trim());
        if (patch.has("amount")) spec = spec.param("amount", patch.amount());
        if (patch.has("dueOn")) spec = spec.param("dueOn", patch.dueOn());
        if (patch.has("comment")) spec = spec.param("comment", patch.comment());
        spec.update();
    }
    private static final String PAYMENT_COLUMNS = "SELECT id, organization_id, charge_id, athlete_id, section_id, amount, paid_on, method, comment, status, created_by, created_at, voided_by, voided_at, void_reason FROM payment WHERE organization_id = :org AND id = :id";
    Optional<Payment> payment(UUID org, UUID id) { return jdbc.sql(PAYMENT_COLUMNS).param("org", org).param("id", id).query((rs, row) -> payment(rs)).optional(); }
    Optional<Payment> lockPayment(UUID org, UUID id) { return jdbc.sql(PAYMENT_COLUMNS + " FOR UPDATE").param("org", org).param("id", id).query((rs, row) -> payment(rs)).optional(); }
    void voidPayment(UUID org, UUID id, String reason, UUID user) {
        jdbc.sql("UPDATE payment SET status = 'VOIDED', voided_by = :user, voided_at = CURRENT_TIMESTAMP, void_reason = :reason WHERE organization_id = :org AND id = :id")
                .param("user", user).param("reason", reason).param("org", org).param("id", id).update();
    }
    // Журнал действий (главная панель): финансовые исправления фиксируются (ТЗ, Б8)
    void recordActivity(UUID org, UUID actor, String action, String entityType, UUID entityId, String title) {
        String text = title.length() > 255 ? title.substring(0, 254) + "…" : title;
        jdbc.sql("INSERT INTO activity (organization_id, actor_id, action, entity_type, entity_id, title) VALUES (:org, :actor, :action, :entityType, :entityId, :title)")
                .param("org", org).param("actor", actor).param("action", action).param("entityType", entityType)
                .param("entityId", entityId).param("title", text).update();
    }
record Membership(String roles, String permissions) { boolean role(String v) { return roles.contains(v); } boolean permission(String v) { return permissions.contains(v); } }
}