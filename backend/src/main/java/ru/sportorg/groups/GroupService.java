package ru.sportorg.groups;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sportorg.auth.AuthenticatedUser;
import ru.sportorg.organizations.OrganizationNotFoundException;
import ru.sportorg.organizations.OrganizationPermissionException;
import ru.sportorg.organizations.OrganizationRequestException;

@Service
class GroupService {
    private static final List<String> STATUSES = List.of("ACTIVE", "ARCHIVED");
    private final GroupRepository repository;
    private final Clock clock;

    GroupService(GroupRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    SectionPage sections(AuthenticatedUser actor, UUID org, String q, UUID sportTypeId, String status, int page, int size) {
        require(actor, org, "sections.read"); validatePage(page, size); validateStatus(status);
        String query = normalize(q); long total = repository.countSections(org, query, sportTypeId, status);
        return new SectionPage(repository.sections(org, query, sportTypeId, status, size, page * size), page, size, total, pages(total, size));
    }

    @Transactional Section createSection(AuthenticatedUser actor, UUID org, SectionWrite write) {
        requireTrainer(actor, org, "sections.write"); validateStatus(write.status());
        if (blank(write.name()) || write.sportTypeId() == null) throw new OrganizationRequestException("Некорректная секция.");
        try {
            return repository.insertSection(org, write, clock.instant());
        } catch (DuplicateKeyException e) {
            throw new OrganizationRequestException("Секция с таким названием уже существует.");
        }
    }

    @Transactional Section patchSection(AuthenticatedUser actor, UUID org, UUID id, SectionPatch patch) {
        requireTrainer(actor, org, "sections.write"); if (patch.isEmpty()) throw new OrganizationRequestException("PATCH должен содержать хотя бы одно поле.");
        repository.section(org, id).orElseThrow(OrganizationNotFoundException::new); if (patch.has("status")) validateStatus(patch.status()); if (patch.has("name") && blank(patch.name())) throw new OrganizationRequestException("Название секции не может быть пустым.");
        repository.patchSection(org, id, patch, clock.instant()); return repository.section(org, id).orElseThrow();
    }

    GroupPage groups(AuthenticatedUser actor, UUID org, String q, UUID sectionId, UUID coachId, UUID athleteId, String status, int page, int size) {
        Access access = require(actor, org, "groups.read"); validatePage(page, size); validateStatus(status); String query = normalize(q);
        UUID scopeUser = access.self ? actor.userId() : null; long total = repository.countGroups(org, query, sectionId, coachId, athleteId, status, scopeUser, access.scope);
        return new GroupPage(repository.groups(org, query, sectionId, coachId, athleteId, status, scopeUser, access.scope, size, page * size), page, size, total, pages(total, size));
    }

    @Transactional Group createGroup(AuthenticatedUser actor, UUID org, GroupWrite write) {
        requireTrainer(actor, org, "groups.write");
        validateGroupWrite(org, write);
        try {
            return repository.insertGroup(org, write, clock.instant());
        } catch (DuplicateKeyException e) {
            throw new OrganizationRequestException("Группа с таким названием уже существует.");
        }
    }

    GroupDetail detail(AuthenticatedUser actor, UUID org, UUID groupId, boolean includeFormer) {
        Access access = require(actor, org, "groups.read"); Group group = repository.group(org, groupId).orElseThrow(OrganizationNotFoundException::new);
        return new GroupDetail(group, repository.athletes(groupId, includeFormer, access.self ? actor.userId() : null, access.scope));
    }

    @Transactional Group patchGroup(AuthenticatedUser actor, UUID org, UUID id, GroupPatch patch) {
        requireTrainer(actor, org, "groups.write"); if (patch.isEmpty()) throw new OrganizationRequestException("PATCH должен содержать хотя бы одно поле.");
        Group group = repository.group(org, id).orElseThrow(OrganizationNotFoundException::new); if (patch.has("status")) validateStatus(patch.status()); if (patch.has("name") && blank(patch.name())) throw new OrganizationRequestException("Название группы не может быть пустым.");
        if (patch.has("coachIds")) validateCoaches(org, patch.coachIds()); if ("ARCHIVED".equals(group.status()) && patch.has("status") && "ACTIVE".equals(patch.status())) throw new OrganizationRequestException("Архивную группу нельзя восстановить.");
        repository.patchGroup(org, id, patch, clock.instant()); return repository.group(org, id).orElseThrow();
    }

    @Transactional GroupAthlete addAthlete(AuthenticatedUser actor, UUID org, UUID groupId, UUID athleteId, LocalDate joinedOn) {
        requireTrainer(actor, org, "groups.write"); if (joinedOn == null || !repository.athleteInOrg(athleteId, org)) throw new OrganizationNotFoundException();
        Group group = repository.group(org, groupId).orElseThrow(OrganizationNotFoundException::new); if (!"ACTIVE".equals(group.status())) throw new OrganizationRequestException("В архивную группу нельзя зачислять спортсменов."); if (repository.groupOpen(groupId, athleteId)) throw new OrganizationRequestException("Спортсмен уже зачислен в группу.");
        try {
            repository.addAthlete(org, groupId, athleteId, joinedOn);
        } catch (DuplicateKeyException e) {
            throw new OrganizationRequestException("Период участия уже существует.");
        }
        return repository.athletes(groupId, true, null, null).stream().filter(item -> item.athleteId().equals(athleteId) && item.joinedOn().equals(joinedOn)).findFirst().orElseThrow();
    }

    @Transactional GroupAthlete leaveAthlete(AuthenticatedUser actor, UUID org, UUID groupId, UUID athleteId, LocalDate leftOn) {
        requireTrainer(actor, org, "groups.write"); if (leftOn == null || !repository.groupOpen(groupId, athleteId)) throw new OrganizationNotFoundException();
        repository.leaveAthlete(groupId, athleteId, leftOn); return repository.athletes(groupId, true, null, null).stream().filter(item -> item.athleteId().equals(athleteId) && item.leftOn() != null).reduce((first, second) -> second).orElseThrow();
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
        return new Access(parent || athlete, scope);
    }

    private void requireTrainer(AuthenticatedUser actor, UUID org, String permission) {
        require(actor, org, permission);
        var membership = repository.membership(actor.userId(), org)
                .orElseThrow(OrganizationNotFoundException::new);
        if (!membership.role("TRAINER")) {
            throw new OrganizationPermissionException();
        }
    }

    private void validateGroupWrite(UUID org, GroupWrite write) {
        validateStatus(write.status());
        if (blank(write.name())
                || write.sectionId() == null
                || write.coachIds() == null
                || write.coachIds().isEmpty()
                || !repository.sectionActive(write.sectionId(), org)) {
            throw new OrganizationRequestException("Некорректная группа или неактивная секция.");
        }
        validateCoaches(org, write.coachIds());
    }

    private void validateCoaches(UUID org, List<UUID> ids) {
        if (ids == null
                || ids.isEmpty()
                || ids.size() != ids.stream().distinct().count()
                || ids.stream().anyMatch(id -> !repository.activeRole(id, org, "TRAINER"))) {
            throw new OrganizationRequestException("Все тренеры должны иметь активную роль TRAINER.");
        }
    }

    private void validateStatus(String status) {
        if (status != null && !STATUSES.contains(status)) {
            throw new OrganizationRequestException("Недопустимый статус.");
        }
    }

    private String normalize(String q) {
        return q == null || q.isBlank() ? null : q.trim().toLowerCase(Locale.ROOT);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new OrganizationRequestException("page должен быть неотрицательным, size должен быть от 1 до 100.");
        }
    }
    private int pages(long total, int size) {
        return (int) Math.ceil((double) total / size);
    }
    private record Access(boolean self, String scope) { }
}