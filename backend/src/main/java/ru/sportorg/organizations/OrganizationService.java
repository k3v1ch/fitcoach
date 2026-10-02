package ru.sportorg.organizations;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

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
            try {
                if (!ZoneId.getAvailableZoneIds().contains(patch.getTimezone())) {
                    throw new OrganizationRequestException("Укажите корректную IANA timezone.");
                }
                ZoneId.of(patch.getTimezone());
            } catch (DateTimeException | NullPointerException exception) {
                throw new OrganizationRequestException("Укажите корректную IANA timezone.");
            }
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