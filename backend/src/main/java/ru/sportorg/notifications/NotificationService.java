package ru.sportorg.notifications;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import ru.sportorg.auth.AuthenticatedUser;
import ru.sportorg.organizations.OrganizationNotFoundException;
import ru.sportorg.organizations.OrganizationRequestException;

/**
 * Уведомления в веб-приложении (ТЗ: изменение и отмена тренировки с уведомлением участников, Ф7.7 — мероприятия).
 * Создаются в транзакции изменения, поэтому откат изменения откатывает и уведомление.
 */
@Service
public class NotificationService {
    private static final Locale RU = Locale.forLanguageTag("ru");
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMMM, HH:mm", RU);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMMM", RU);

    private final NotificationRepository repository;
    private final Clock clock;

    NotificationService(NotificationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    // --- для модулей, которые создают уведомления ---

    public List<UUID> trainingAudience(UUID trainingId) {
        return repository.trainingAudience(trainingId);
    }

    public List<UUID> eventAudience(UUID eventId) {
        return repository.eventAudience(eventId);
    }

    public List<UUID> athletesAudience(Collection<UUID> athleteIds) {
        return repository.athletesAudience(athleteIds);
    }

    /** Автор изменения уведомление о нём не получает; получатели без активного членства пропускаются. */
    public void notifyUsers(UUID org, Collection<UUID> users, UUID actorId, String type, String title, String text,
                            String entityType, UUID entityId) {
        Set<UUID> unique = new LinkedHashSet<>(users);
        unique.remove(actorId);
        unique.remove(null);
        if (unique.isEmpty()) return;
        repository.insert(org, new ArrayList<>(unique), type, title, text, entityType, entityId, clock.instant(), false);
    }

    /** Дата и время в часовом поясе организации: «5 октября, 10:00». */
    public String when(UUID org, Instant instant) {
        return instant == null ? "" : WHEN.format(instant.atZone(zone(org)));
    }

    public String day(LocalDate date) {
        return date == null ? "" : DAY.format(date);
    }

    /** Напоминание о мероприятии, до начала которого осталось не больше ahead; каждому — один раз. */
    public int remindUpcomingEvents(Duration ahead) {
        Instant now = clock.instant();
        int created = 0;
        for (NotificationRepository.UpcomingEvent event : repository.upcomingEvents(now, now.plus(ahead))) {
            List<UUID> users = repository.eventAudience(event.id());
            created += repository.insert(event.organizationId(), users, "EVENT_REMINDER", "Скоро мероприятие",
                    "«" + event.title() + "» начнётся " + when(event.organizationId(), event.startsAt()) + ".",
                    "EVENT", event.id(), now, true);
        }
        return created;
    }

    // --- лента текущего пользователя ---

    NotificationPage list(AuthenticatedUser actor, UUID org, boolean unread, int page, int size) {
        requireMember(actor, org);
        if (page < 0 || size < 1 || size > 100) {
            throw new OrganizationRequestException("page должен быть неотрицательным, size должен быть от 1 до 100.");
        }
        long total = repository.count(org, actor.userId(), unread);
        return new NotificationPage(repository.find(org, actor.userId(), unread, size, page * size), page, size, total,
                (int) Math.ceil((double) total / size), repository.unreadCount(org, actor.userId()));
    }

    void read(AuthenticatedUser actor, UUID org, UUID id) {
        requireMember(actor, org);
        if (repository.markRead(org, actor.userId(), id, clock.instant()) == 0) throw new OrganizationNotFoundException();
    }

    void readAll(AuthenticatedUser actor, UUID org) {
        requireMember(actor, org);
        repository.markAllRead(org, actor.userId(), clock.instant());
    }

    private void requireMember(AuthenticatedUser actor, UUID org) {
        if (actor == null || !repository.member(org, actor.userId())) throw new OrganizationNotFoundException();
    }

    private ZoneId zone(UUID org) {
        try {
            String tz = repository.timezone(org);
            return tz == null ? ZoneId.of("Europe/Moscow") : ZoneId.of(tz);
        } catch (DateTimeException e) {
            return ZoneId.of("Europe/Moscow");
        }
    }
}
