package ru.sportorg.notifications;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Ф7.7: напоминание участникам о мероприятии, которое начнётся в ближайшие 48 часов. */
@Component
@ConditionalOnProperty(name = "app.notifications.reminders", havingValue = "true", matchIfMissing = true)
class EventReminderJob {
    private static final Logger log = LoggerFactory.getLogger(EventReminderJob.class);
    private final NotificationService notifications;

    EventReminderJob(NotificationService notifications) {
        this.notifications = notifications;
    }

    @Scheduled(initialDelayString = "PT2M", fixedDelayString = "PT30M")
    void remind() {
        try {
            int created = notifications.remindUpcomingEvents(Duration.ofHours(48));
            if (created > 0) log.info("event reminders created: {}", created);
        } catch (RuntimeException e) {
            log.warn("event reminders failed", e);
        }
    }
}
