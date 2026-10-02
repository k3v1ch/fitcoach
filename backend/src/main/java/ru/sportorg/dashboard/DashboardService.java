package ru.sportorg.dashboard;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import ru.sportorg.auth.AuthenticatedUser;
import ru.sportorg.organizations.OrganizationNotFoundException;
import ru.sportorg.organizations.OrganizationPermissionException;
import ru.sportorg.organizations.OrganizationRequestException;

@Service
class DashboardService {
    private final DashboardRepository repository;
    DashboardService(DashboardRepository repository) {
        this.repository = repository;
    }

    Dashboard get(
            AuthenticatedUser actor,
            UUID org,
            String view,
            UUID athleteId,
            LocalDate from,
            LocalDate to,
            int activityPage,
            int activitySize) {
        if (actor == null || !repository.organizationExists(org)) {
            throw new OrganizationNotFoundException();
        }
        var membership = repository.membership(actor.userId(), org);
        if (membership == null) {
            throw new OrganizationNotFoundException();
        }
        if (!List.of("TRAINER", "PARENT", "ATHLETE", "AGENCY").contains(view)) {
            throw new OrganizationRequestException("Недопустимый вид dashboard.");
        }
        boolean self = membership.role("PARENT") || membership.role("ATHLETE");
        if (self && (athleteId == null || !repository.athleteVisible(org, athleteId, actor.userId()))) {
            throw new OrganizationNotFoundException();
        }
        if (!self && !membership.role(view)) {
            throw new OrganizationPermissionException();
        }
        if (from == null || to == null || from.isAfter(to)
                || activityPage < 0 || activitySize < 1 || activitySize > 100) {
            throw new OrganizationRequestException("Некорректные границы dashboard.");
        }

        UUID scope = self ? athleteId : null;
        int athletes = repository.athletes(org, scope);
        int groups = repository.groups(org, scope);
        Instant fromInstant = from.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        Instant toInstant = to.plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        int upcomingTrainings = repository.upcomingTrainings(org, fromInstant, toInstant, scope);
        int upcomingEvents = repository.upcomingEvents(org, from, to, scope);
        Dashboard.Finance finance = membership.permission("finance.read")
                || self && membership.permission("charges.read")
                ? map(repository.finance(org, scope))
                : null;
        long total = repository.activityCount(org);

        return new Dashboard(
                org,
                view,
                athleteId,
                new Dashboard.Counters(athletes, groups, upcomingTrainings, upcomingEvents),
                finance,
                repository.nextTrainings(org, Instant.now(), toInstant, scope),
                repository.nextEvents(org, from, to, scope),
                new Dashboard.ActivityPage(
                        repository.activities(org, activitySize, activityPage * activitySize),
                        activityPage,
                        activitySize,
                        total,
                        (int) Math.ceil((double) total / activitySize)));
    }

    private Dashboard.Finance map(DashboardRepository.Finance value) {
        return new Dashboard.Finance(value.outstanding(), value.overdue());
    }
}