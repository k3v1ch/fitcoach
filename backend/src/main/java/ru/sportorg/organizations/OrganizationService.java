package ru.sportorg.organizations;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import ru.sportorg.access.MembershipPolicy;
import ru.sportorg.auth.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class OrganizationService {

    private static final Set<String> ROLES = Set.of("TRAINER", "PARENT", "ATHLETE", "AGENCY");
    private static final Set<String> MEMBERSHIP_STATUSES = Set.of("ACTIVE", "BLOCKED");

    private final OrganizationRepository repository;
    private final Clock clock;

    OrganizationService(OrganizationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    List<OrganizationAccess> listOrganizations(AuthenticatedUser actor) {
        if (actor == null) {
            throw new OrganizationNotFoundException();
        }
        return repository.findOrganizationsForUser(actor.userId());
    }

    @Transactional
    Organization createOrganization(AuthenticatedUser actor, OrganizationWrite write) {
        if (actor == null) {
            throw new OrganizationNotFoundException();
        }
        if (write == null || write.name() == null || write.name().isBlank()
                || write.name().trim().length() > 200) {
            throw new OrganizationRequestException("Название организации должно содержать от 1 до 200 символов.");
        }
        String timezone = write.timezone() == null || write.timezone().isBlank()
                ? "Europe/Moscow" : write.timezone().trim();
        validateTimezone(timezone);
        return repository.createOrganization(actor.userId(), write.name().trim(), write.description(),
                write.address(), timezone, MembershipPolicy.trainerPermissions(), clock.instant());
    }

    Organization getOrganization(AuthenticatedUser actor, UUID organizationId) {
        Organization organization = findOrganization(organizationId);
        requireMembership(actor, organizationId);
        return organization;
    }

    @Transactional
    Organization patchOrganization(AuthenticatedUser actor, UUID organizationId, OrganizationPatch patch) {
        findOrganization(organizationId);
        OrganizationRepository.MembershipAccess access = requireMembership(actor, organizationId);
        if (!access.roles().contains("TRAINER") || !access.permissions().contains("organization.write")) {
            throw new OrganizationPermissionException();
        }
        validatePatch(patch);
        repository.patchOrganization(organizationId, patch, clock.instant());
        return findOrganization(organizationId);
    }

    OrganizationMemberPage getMembers(AuthenticatedUser actor, UUID organizationId, String q,
                                       String role, String status, int page, int size) {
        requirePermission(actor, organizationId, "members.read");
        if (page < 0 || size < 1 || size > 100) {
            throw new OrganizationRequestException("page должен быть неотрицательным, size должен быть от 1 до 100.");
        }
        if (role != null && !ROLES.contains(role)) {
            throw new OrganizationRequestException("Недопустимая роль.");
        }
        if (status != null && !MEMBERSHIP_STATUSES.contains(status)) {
            throw new OrganizationRequestException("Недопустимый статус членства.");
        }
        String normalizedQuery = q == null || q.isBlank() ? null : q.trim().toLowerCase(Locale.ROOT);
        long total = repository.countMembers(organizationId, normalizedQuery, role, status);
        List<OrganizationMember> items = repository.findMembers(
                organizationId, normalizedQuery, role, status, size, page * size);
        return new OrganizationMemberPage(items, page, size, total, (int) Math.ceil((double) total / size));
    }

    @Transactional
    OrganizationMember addParent(AuthenticatedUser actor, UUID organizationId, ParentMembershipWrite write) {
        findOrganization(organizationId);
        OrganizationRepository.MembershipAccess access = requireMembership(actor, organizationId);
        if (!access.roles().contains("TRAINER") || !access.permissions().contains("members.write")) {
            throw new OrganizationPermissionException();
        }
        if (write.email() == null || write.email().isBlank() || write.email().trim().length() > 320) {
            throw new OrganizationRequestException("Укажите корректный email родителя.");
        }
        String normalizedEmail = write.email().trim().toLowerCase(Locale.ROOT);
        return repository.addParentMembership(organizationId, normalizedEmail,
                        MembershipPolicy.selfServicePermissions(), clock.instant())
                .orElseThrow(() -> new OrganizationRequestException(
                        "Аккаунт не найден или не активирован, либо членство нельзя изменить."));
    }

    private void validatePatch(OrganizationPatch patch) {
        if (!patch.isNameProvided() && !patch.isDescriptionProvided()
                && !patch.isAddressProvided() && !patch.isTimezoneProvided()) {
            throw new OrganizationRequestException("PATCH должен содержать хотя бы одно поле.");
        }
        if (patch.isNameProvided() && (patch.getName() == null || patch.getName().isBlank()
                || patch.getName().trim().length() > 200)) {
            throw new OrganizationRequestException("Название организации не может быть пустым.");
        }
        if (patch.isTimezoneProvided()) {
            validateTimezone(patch.getTimezone());
        }
    }

    private void validateTimezone(String timezone) {
        try {
            if (timezone == null || !ZoneId.getAvailableZoneIds().contains(timezone)) {
                throw new OrganizationRequestException("Укажите корректную IANA timezone.");
            }
            ZoneId.of(timezone);
        } catch (DateTimeException | NullPointerException exception) {
            throw new OrganizationRequestException("Укажите корректную IANA timezone.");
        }
    }

    private Organization findOrganization(UUID organizationId) {
        return repository.findOrganization(organizationId)
                .orElseThrow(OrganizationNotFoundException::new);
    }

    private void requirePermission(AuthenticatedUser actor, UUID organizationId, String permission) {
        findOrganization(organizationId);
        OrganizationRepository.MembershipAccess access = requireMembership(actor, organizationId);
        if (!access.permissions().contains(permission)) {
            throw new OrganizationPermissionException();
        }
    }

    private OrganizationRepository.MembershipAccess requireMembership(AuthenticatedUser actor, UUID organizationId) {
        if (actor == null) {
            throw new OrganizationNotFoundException();
        }
        return repository.findActiveMembership(actor.userId(), organizationId)
                .orElseThrow(OrganizationNotFoundException::new);
    }
}