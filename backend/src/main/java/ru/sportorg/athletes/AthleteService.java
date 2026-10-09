package ru.sportorg.athletes;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sportorg.auth.AuthenticatedUser;
import ru.sportorg.organizations.OrganizationNotFoundException;
import ru.sportorg.organizations.OrganizationPermissionException;
import ru.sportorg.organizations.OrganizationRequestException;

@Service
class AthleteService {

    private static final List<String> STATUSES = List.of("ACTIVE", "ARCHIVED");
    private final AthleteRepository repository;
    private final Clock clock;

    AthleteService(AthleteRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    AthletePage find(AuthenticatedUser actor, UUID organizationId, String q, String status, int page, int size) {
        Access access = requireAccess(actor, organizationId, "athletes.read");
        validatePage(page, size);
        if (status != null && !STATUSES.contains(status)) throw new OrganizationRequestException("Недопустимый статус спортсмена.");
        String normalized = q == null || q.isBlank() ? null : q.trim().toLowerCase(Locale.ROOT);
        UUID scopeUserId = access.selfScope() ? actor.userId() : null;
        String scope = access.scope();
        long total = repository.count(organizationId, normalized, status, scopeUserId, scope);
        List<Athlete> items = repository.find(organizationId, normalized, status, scopeUserId, scope, size, page * size);
        return new AthletePage(items, page, size, total, (int) Math.ceil((double) total / size));
    }

    // Родители организации с детьми (6.6, №017): нужен доступ ко всему списку спортсменов — не для родителя и спортсмена
    ParentPage parents(AuthenticatedUser actor, UUID organizationId, String q, UUID athleteId, int page, int size) {
        Access access = requireAccess(actor, organizationId, "athletes.read");
        if (access.selfScope()) throw new OrganizationPermissionException();
        validatePage(page, size);
        String normalized = q == null || q.isBlank() ? null : q.trim().toLowerCase(Locale.ROOT);
        long total = repository.countParents(organizationId, normalized, athleteId);
        return new ParentPage(repository.parents(organizationId, normalized, athleteId, size, page * size),
                page, size, total, (int) Math.ceil((double) total / size));
    }

    Athlete get(AuthenticatedUser actor, UUID organizationId, UUID athleteId) {
        Access access = requireAccess(actor, organizationId, "athletes.read");
        Athlete athlete = find(organizationId, athleteId);
        if (access.selfScope() && !canSeeSelf(access.scope(), actor.userId(), athlete)) throw new OrganizationNotFoundException();
        return athlete;
    }

    @Transactional
    Athlete create(AuthenticatedUser actor, UUID organizationId, AthleteWrite write) {
        Access access = requireAccess(actor, organizationId, "athletes.write");
        if (!access.trainer()) throw new OrganizationPermissionException();
        validateWrite(write);
        validateAthleteUser(organizationId, write.userId());
        try {
            return repository.insert(organizationId, write, clock.instant());
        } catch (org.springframework.dao.DuplicateKeyException exception) {
            throw new OrganizationRequestException("У аккаунта уже есть карточка спортсмена в этой организации.");
        }
    }

    @Transactional
    Athlete patch(AuthenticatedUser actor, UUID organizationId, UUID athleteId, AthletePatch patch) {
        Access access = requireAccess(actor, organizationId, "athletes.write");
        if (!access.trainer()) throw new OrganizationPermissionException();
        if (patch.isEmpty()) throw new OrganizationRequestException("PATCH должен содержать хотя бы одно поле.");
        find(organizationId, athleteId);
        if (patch.has("firstName") && blank(patch.firstName()) || patch.has("lastName") && blank(patch.lastName())) {
            throw new OrganizationRequestException("Имя и фамилия не могут быть пустыми.");
        }
        if (patch.has("status") && !STATUSES.contains(patch.status())) throw new OrganizationRequestException("Недопустимый статус спортсмена.");
        if (patch.has("userId")) validateAthleteUser(organizationId, patch.userId());
        if (patch.has("parentLinks")) {
            if (!access.membersWrite()) throw new OrganizationPermissionException();
            if (patch.parentLinks() == null) throw new OrganizationRequestException("parentLinks не может быть null.");
            for (ParentLinkWrite link : patch.parentLinks()) {
                if (!repository.hasActiveRole(link.parentUserId(), organizationId, "PARENT")) {
                    throw new OrganizationRequestException("Все родители должны иметь активную роль PARENT в организации.");
                }
            }
        }
        repository.patch(organizationId, athleteId, patch, clock.instant());
        if (patch.has("parentLinks")) repository.replaceParentLinks(organizationId, athleteId, patch.parentLinks());
        return find(organizationId, athleteId);
    }

    private Access requireAccess(AuthenticatedUser actor, UUID organizationId, String permission) {
        if (actor == null || !repository.organizationExists(organizationId)) throw new OrganizationNotFoundException();
        AthleteRepository.MembershipAccess membership = repository.findActiveMembership(actor.userId(), organizationId)
                .orElseThrow(OrganizationNotFoundException::new);
        if (!membership.hasPermission(permission)) throw new OrganizationPermissionException();
        boolean parent = membership.hasRole("PARENT");
        boolean athlete = membership.hasRole("ATHLETE");
        if (parent || athlete) {
            String scope = parent && athlete ? "BOTH" : parent ? "PARENT" : "ATHLETE";
            return new Access(false, true, scope, membership.hasPermission("members.write"));
        }
        return new Access(membership.hasRole("TRAINER"), false, null, membership.hasPermission("members.write"));
    }

    private Athlete find(UUID organizationId, UUID athleteId) {
        return repository.findById(organizationId, athleteId).orElseThrow(OrganizationNotFoundException::new);
    }

    private void validateAthleteUser(UUID organizationId, UUID userId) {
        if (userId != null && !repository.hasActiveRole(userId, organizationId, "ATHLETE")) {
            throw new OrganizationRequestException("Аккаунт спортсмена должен иметь активную роль ATHLETE.");
        }
    }

    private void validateWrite(AthleteWrite write) {
        if (blank(write.firstName()) || blank(write.lastName()) || write.birthDate() == null || write.enrolledOn() == null
                || !STATUSES.contains(write.status())) throw new OrganizationRequestException("Некорректные данные спортсмена.");
    }

    private boolean canSeeSelf(String scope, UUID userId, Athlete athlete) {
        boolean parentAccess = ("PARENT".equals(scope) || "BOTH".equals(scope))
            && athlete.parents().stream().anyMatch(link -> link.parentUserId().equals(userId));
        boolean athleteAccess = ("ATHLETE".equals(scope) || "BOTH".equals(scope)) && userId.equals(athlete.userId());
        return parentAccess || athleteAccess;
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new OrganizationRequestException("page должен быть неотрицательным, size должен быть от 1 до 100.");
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
    private record Access(boolean trainer, boolean selfScope, String scope, boolean membersWrite) { }
}