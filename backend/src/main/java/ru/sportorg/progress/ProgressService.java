package ru.sportorg.progress;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sportorg.auth.AuthenticatedUser;
import ru.sportorg.organizations.OrganizationNotFoundException;
import ru.sportorg.organizations.OrganizationPermissionException;
import ru.sportorg.organizations.OrganizationRequestException;

@Service
class ProgressService {
    private final ProgressRepository repository;
    private final Clock clock;
    ProgressService(ProgressRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    ResultPage results(AuthenticatedUser actor, UUID org, UUID athleteId, LocalDate from, LocalDate to, String metricName, String unit, int page, int size) {
        Access access = require(actor, org, "progress.read"); visible(access, actor, org, athleteId); page(page, size); if (from != null && to != null && from.isAfter(to)) throw new OrganizationRequestException("from не может быть позже to.");
        String metric = metricName == null || metricName.isBlank() ? null : metricName.trim().toLowerCase(); long total = repository.resultCount(org, athleteId, from, to, metric, unit); return new ResultPage(repository.results(org, athleteId, from, to, metric, unit, size, page * size), page, size, total, pages(total, size));
    }
    @Transactional
    Result create(AuthenticatedUser actor, UUID org, UUID athleteId, ResultWrite write) {
        requireTrainer(actor, org, "progress.write");
        visibleOrg(org, athleteId);
        validate(write.metricName(), write.unit(), write.value() == null, write.measuredOn());
        return repository.insertResult(org, athleteId, write, actor.userId(), clock.instant());
    }

    @Transactional
    Result patch(AuthenticatedUser actor, UUID org, UUID athleteId, UUID resultId, ResultPatch patch) {
        requireTrainer(actor, org, "progress.write");
        visibleOrg(org, athleteId);
        if (patch.empty()) {
            throw new OrganizationRequestException("PATCH должен содержать хотя бы одно поле.");
        }
        if (patch.has("metricName") && blank(patch.metricName())
                || patch.has("unit") && blank(patch.unit())
                || patch.has("value") && patch.value() == null
                || patch.has("measuredOn") && patch.measuredOn() == null) {
            throw new OrganizationRequestException("Некорректные поля результата.");
        }
        repository.resultById(org, athleteId, resultId).orElseThrow(OrganizationNotFoundException::new);
        repository.patchResult(org, athleteId, resultId, patch, clock.instant());
        return repository.resultById(org, athleteId, resultId).orElseThrow();
    }

    @Transactional
    void delete(AuthenticatedUser actor, UUID org, UUID athleteId, UUID resultId) {
        requireTrainer(actor, org, "progress.write");
        visibleOrg(org, athleteId);
        repository.resultById(org, athleteId, resultId).orElseThrow(OrganizationNotFoundException::new);
        repository.deleteResult(org, athleteId, resultId);
    }

    PageData<AthleteStandard> standards(AuthenticatedUser actor, UUID org, UUID athleteId, int page, int size) {
        Access access = require(actor, org, "progress.read");
        visible(access, actor, org, athleteId);
        page(page, size);
        long total = repository.standardCount(org, athleteId);
        return new PageData<>(
                repository.standards(org, athleteId, size, page * size),
                page,
                size,
                total,
                pages(total, size));
    }

    PageData<AthleteRank> ranks(AuthenticatedUser actor, UUID org, UUID athleteId, int page, int size) {
        Access access = require(actor, org, "progress.read");
        visible(access, actor, org, athleteId);
        page(page, size);
        long total = repository.rankCount(org, athleteId);
        return new PageData<>(
                repository.ranks(org, athleteId, size, page * size),
                page,
                size,
                total,
                pages(total, size));
    }

    private Access require(AuthenticatedUser actor, UUID org, String permission) {
        if (actor == null || !repository.organizationExists(org)) {
            throw new OrganizationNotFoundException();
        }
        var membership = repository.membership(actor.userId(), org)
                .orElseThrow(OrganizationNotFoundException::new);
        if (!membership.permission(permission)) {
            throw new OrganizationPermissionException();
        }
        boolean parent = membership.role("PARENT");
        boolean athlete = membership.role("ATHLETE");
        String scope = parent && athlete ? "BOTH" : parent ? "PARENT" : athlete ? "ATHLETE" : null;
        return new Access(parent || athlete, membership.role("TRAINER"), scope);
    }

    private void requireTrainer(AuthenticatedUser actor, UUID org, String permission) {
        if (!require(actor, org, permission).trainer()) {
            throw new OrganizationPermissionException();
        }
    }

    private void visible(Access access, AuthenticatedUser actor, UUID org, UUID athleteId) {
        if (!repository.athleteExists(org, athleteId)
                || access.self() && !repository.athleteVisible(org, athleteId, actor.userId(), access.scope())) {
            throw new OrganizationNotFoundException();
        }
    }

    private void visibleOrg(UUID org, UUID athleteId) {
        if (!repository.athleteExists(org, athleteId)) {
            throw new OrganizationNotFoundException();
        }
    }

    private void validate(String metric, String unit, boolean valueNull, LocalDate date) {
        if (blank(metric) || blank(unit) || valueNull || date == null) {
            throw new OrganizationRequestException("Некорректные поля результата.");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private void page(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new OrganizationRequestException("page должен быть неотрицательным, size должен быть от 1 до 100.");
        }
    }

    private int pages(long total, int size) {
        return (int) Math.ceil((double) total / size);
    }
    private record Access(boolean self, boolean trainer, String scope) { }
    record PageData<T>(List<T> items, int page, int size, long totalElements, int totalPages) { }
}