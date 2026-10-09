package ru.sportorg.notifications;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.sportorg.jdbc.JdbcTime;

@Repository
class NotificationRepository {
    // Состав группы на дату тренировки — как в расписании (timezone организации)
    private static final String TRAINING_ATHLETES = "SELECT ga.athlete_id FROM training t JOIN organization o ON o.id = t.organization_id"
            + " JOIN group_athlete ga ON ga.group_id = t.group_id AND ga.joined_on <= (t.starts_at AT TIME ZONE o.timezone)::date"
            + " AND (ga.left_on IS NULL OR ga.left_on >= (t.starts_at AT TIME ZONE o.timezone)::date) WHERE t.id = :id";

    private final JdbcClient jdbc;

    NotificationRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    boolean member(UUID org, UUID userId) {
        return jdbc.sql("SELECT count(*) FROM membership WHERE organization_id = :org AND user_id = :userId AND status = 'ACTIVE'")
                .param("org", org).param("userId", userId).query(Long.class).single() > 0;
    }

    String timezone(UUID org) {
        return jdbc.sql("SELECT timezone FROM organization WHERE id = :org").param("org", org).query(String.class).optional().orElse(null);
    }

    // Аккаунты спортсменов и их родителей
    List<UUID> athletesAudience(Collection<UUID> athleteIds) {
        if (athleteIds.isEmpty()) return List.of();
        return jdbc.sql("SELECT a.user_id FROM athlete a WHERE a.id IN (:ids) AND a.user_id IS NOT NULL"
                        + " UNION SELECT pl.parent_user_id FROM parent_link pl WHERE pl.athlete_id IN (:ids)")
                .param("ids", athleteIds).query(UUID.class).list();
    }

    // Спортсмены группы на дату тренировки, их родители и тренеры тренировки
    List<UUID> trainingAudience(UUID trainingId) {
        return jdbc.sql("SELECT a.user_id FROM athlete a WHERE a.id IN (" + TRAINING_ATHLETES + ") AND a.user_id IS NOT NULL"
                        + " UNION SELECT pl.parent_user_id FROM parent_link pl WHERE pl.athlete_id IN (" + TRAINING_ATHLETES + ")"
                        + " UNION SELECT tc.coach_id FROM training_coach tc WHERE tc.training_id = :id")
                .param("id", trainingId).query(UUID.class).list();
    }

    List<UUID> eventAudience(UUID eventId) {
        return jdbc.sql("SELECT a.user_id FROM event_participant ep JOIN athlete a ON a.id = ep.athlete_id"
                        + " WHERE ep.event_id = :id AND a.user_id IS NOT NULL"
                        + " UNION SELECT pl.parent_user_id FROM event_participant ep JOIN parent_link pl ON pl.athlete_id = ep.athlete_id"
                        + " WHERE ep.event_id = :id")
                .param("id", eventId).query(UUID.class).list();
    }

    /** Только активным участникам организации; once — не повторять тот же тип по той же записи (напоминания). */
    int insert(UUID org, List<UUID> users, String type, String title, String text, String entityType, UUID entityId,
               Instant now, boolean once) {
        if (users.isEmpty()) return 0;
        return jdbc.sql("INSERT INTO notification (organization_id, user_id, type, title, text, entity_type, entity_id, created_at)"
                        + " SELECT m.organization_id, m.user_id, :type, :title, :text, :entityType, :entityId, :now FROM membership m"
                        + " WHERE m.organization_id = :org AND m.status = 'ACTIVE' AND m.user_id IN (:users)"
                        + " AND (:once = FALSE OR NOT EXISTS (SELECT 1 FROM notification n WHERE n.entity_id = :entityId"
                        + " AND n.type = :type AND n.user_id = m.user_id))")
                .param("org", org).param("users", users).param("type", type).param("title", title).param("text", text)
                .param("entityType", entityType).param("entityId", entityId).param("now", JdbcTime.toOffsetDateTime(now))
                .param("once", once).update();
    }

    long count(UUID org, UUID userId, boolean unread) {
        return jdbc.sql("SELECT count(*) FROM notification WHERE organization_id = :org AND user_id = :userId"
                        + " AND (:unread = FALSE OR read_at IS NULL)")
                .param("org", org).param("userId", userId).param("unread", unread).query(Long.class).single();
    }

    long unreadCount(UUID org, UUID userId) {
        return count(org, userId, true);
    }

    List<Notification> find(UUID org, UUID userId, boolean unread, int limit, int offset) {
        return jdbc.sql("SELECT id, type, title, text, entity_type, entity_id, created_at, read_at FROM notification"
                        + " WHERE organization_id = :org AND user_id = :userId AND (:unread = FALSE OR read_at IS NULL)"
                        + " ORDER BY created_at DESC, id DESC LIMIT :limit OFFSET :offset")
                .param("org", org).param("userId", userId).param("unread", unread).param("limit", limit).param("offset", offset)
                .query((rs, row) -> new Notification(rs.getObject("id", UUID.class), rs.getString("type"), rs.getString("title"),
                        rs.getString("text"), rs.getString("entity_type"), rs.getObject("entity_id", UUID.class),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("read_at") == null ? null : rs.getTimestamp("read_at").toInstant()))
                .list();
    }

    int markRead(UUID org, UUID userId, UUID id, Instant now) {
        return jdbc.sql("UPDATE notification SET read_at = COALESCE(read_at, :now) WHERE organization_id = :org AND user_id = :userId AND id = :id")
                .param("now", JdbcTime.toOffsetDateTime(now)).param("org", org).param("userId", userId).param("id", id).update();
    }

    int markAllRead(UUID org, UUID userId, Instant now) {
        return jdbc.sql("UPDATE notification SET read_at = :now WHERE organization_id = :org AND user_id = :userId AND read_at IS NULL")
                .param("now", JdbcTime.toOffsetDateTime(now)).param("org", org).param("userId", userId).update();
    }

    record UpcomingEvent(UUID id, UUID organizationId, String title, Instant startsAt) { }

    List<UpcomingEvent> upcomingEvents(Instant from, Instant to) {
        return jdbc.sql("SELECT id, organization_id, title, starts_at FROM event WHERE status = 'PUBLISHED'"
                        + " AND starts_at > :from AND starts_at <= :to ORDER BY starts_at")
                .param("from", JdbcTime.toOffsetDateTime(from)).param("to", JdbcTime.toOffsetDateTime(to))
                .query((rs, row) -> new UpcomingEvent(rs.getObject("id", UUID.class), rs.getObject("organization_id", UUID.class),
                        rs.getString("title"), rs.getTimestamp("starts_at").toInstant()))
                .list();
    }
}
